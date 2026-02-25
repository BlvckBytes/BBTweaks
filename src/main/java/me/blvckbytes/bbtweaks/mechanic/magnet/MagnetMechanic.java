package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.MVisualizeCommand;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.mechanic.magnet.edit_display.EditDisplayHandler;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicRegistry;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MagnetMechanic extends PredicateMechanic<MagnetInstance> implements Listener {

  private static final Component COMPONENT_PREDICATE_MODE_ON = Component.text("Predicate Mode").color(NamedTextColor.GREEN);
  private static final Component COMPONENT_PREDICATE_MODE_OFF = Component.empty();

  // TODO: Hide visualization of A if editing A
  // TODO: Select param in the UI with any button
  // TODO: Stop visualization on destroy

  protected final CacheByPosition<MagnetInstance> instanceByMountBlockPosition;
  private final CuboidMechanicRegistry<MagnetInstance> instanceCuboidRegistry;
  private final Map<UUID, VisualizationsBucket> visualizationsByPlayerId;
  private final Map<UUID, EditSession> editSessionByPlayerId;

  private final EditDisplayHandler displayHandler;

  private int lastTime;

  public MagnetMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config, PredicateHelper predicateHelper) {
    super(
      plugin, config, predicateHelper,
      new NamespacedKey(plugin, "magnet-filter-predicate"),
      new NamespacedKey(plugin, "magnet-filter-language")
    );

    this.instanceByMountBlockPosition = new CacheByPosition<>();
    this.instanceCuboidRegistry = new CuboidMechanicRegistry<>();
    this.visualizationsByPlayerId = new HashMap<>();
    this.editSessionByPlayerId = new HashMap<>();

    this.displayHandler = new EditDisplayHandler(FloodgateIntegration.load(plugin.getLogger()), config, plugin);

    var visualizeCommandExecutor = new MVisualizeCommand(this, config);

    Objects.requireNonNull(plugin.getCommand("mvisualize")).setExecutor(visualizeCommandExecutor);

    Bukkit.getServer().getPluginManager().registerEvents(displayHandler, plugin);
  }

  public List<MagnetInstance> getMagnetsInChunk(World world, int chunkX, int chunkZ) {
    return instanceCuboidRegistry.getWithinXZChunk(world, chunkX, chunkZ);
  }

  @Override
  public void onMechanicUnload() {
    super.onMechanicUnload();
    instanceCuboidRegistry.clear();
    this.displayHandler.onShutdown();
    HandlerList.unregisterAll(this.displayHandler);
  }

  @Override
  public void tick(int time) {
    super.tick(time);

    this.lastTime = time;

    if (time % config.rootSection.mechanic.magnet.visualization.periodTicks == 0) {
      // TODO: The edit-session should also use this new way of visualization, if it pans out long-term.
      visualizationsByPlayerId.values().forEach(bucket -> bucket.update(time));
      handleVisualizations(editSessionByPlayerId.values());
    }

    if (time % config.rootSection.mechanic.magnet.collectionPeriodTicks == 0)
      handleCollections();
  }

  private void handleCollections() {
    instanceCuboidRegistry.forEachXZChunkBucket((world, chunkX, chunkY, entries) -> {
      if (!world.isChunkLoaded(chunkX, chunkY))
        return;

      var chunk = world.getChunkAt(chunkX, chunkY);

      for (var entity : chunk.getEntities()) {
        if (!(entity instanceof Item item))
          continue;

        // We're explicitly not accounting for the pickup-delay as of now, because magnets do not
        // compete with players about who can pick an item up first - it's a helper-mechanic, and I
        // want to save on as many needless item-entity-ticks as possible, since that's the main goal.

        if (item.isDead())
          continue;

        var itemLocation = item.getLocation();
        var itemStack = item.getItemStack();

        if (itemStack.getAmount() <= 0)
          continue;

        var targetMagnet = instanceCuboidRegistry.lookupClosest(entries, itemLocation, candidate -> candidate.acceptsItem(itemStack));

        if (targetMagnet == null)
          continue;

        targetMagnet.addItem(itemStack);
        item.remove();
      }
    });
  }

  private void handleVisualizations(Collection<? extends VisualizeSession> sessions) {
    for (var visualizationIterator = sessions.iterator(); visualizationIterator.hasNext();) {
      var visualization = visualizationIterator.next();

      if (!isSignRegistered(visualization.sign)) {
        if (visualization instanceof EditSession editSession)
          editSession.cancel();
      }

      if (visualization.isExpired() || !visualization.player.isOnline()) {
        visualizationIterator.remove();
        continue;
      }

      if (visualization instanceof EditSession editSession) {
        var player = editSession.player.getLocation();

        var distanceSquared = -1;

        if (player.getWorld().equals(visualization.sign.getWorld())) {
          var distanceX = visualization.sign.getX() - player.getBlockX();
          var distanceZ = visualization.sign.getZ() - player.getBlockZ();
          distanceSquared = distanceX * distanceX + distanceZ * distanceZ;
        }

        var maxDistance = config.rootSection.mechanic.magnet.visualization.editModeMaxXZDistance;

        if (distanceSquared < 0 || distanceSquared >= maxDistance * maxDistance) {
          config.rootSection.mechanic.magnet.editModeDistanceExceeded.sendMessage(
            editSession.player,
            new InterpretationEnvironment()
              .withVariable("x", visualization.sign.getX())
              .withVariable("y", visualization.sign.getY())
              .withVariable("z", visualization.sign.getZ())
              .withVariable("max_distance", maxDistance)
          );

          editSession.cancel();
          continue;
        }

        if (editSession.clickDetection)
          config.rootSection.mechanic.magnet.editModeClickDetectionActionbar.sendActionBar(editSession.player, editSession.makeEnvironment());

        visualizeEditSessionFor(visualization.player, editSession);
        continue;
      }

      visualizeCuboidFor(visualization.player, visualization.getCuboid());
    }
  }

  private void visualizeEditSessionFor(Player player, EditSession editSession) {
    var stepSize = config.rootSection.mechanic.magnet.visualization.stepSize;

    var normalOptions = new Particle.DustOptions(
      config.rootSection.mechanic.magnet.visualization.editColor._color,
      (float) config.rootSection.mechanic.magnet.visualization.dustSize
    );

    var highlightOptions = new Particle.DustOptions(
      config.rootSection.mechanic.magnet.visualization.editHighlightColor._color,
      (float) config.rootSection.mechanic.magnet.visualization.dustSize
    );

    var currentlySelectedAxis = editSession.getCurrentlySelectedAxis();

    editSession.getCuboid().forEachLine(false, (minX, minY, minZ, maxX, maxY, maxZ, axis) -> {
      var usedOptions = (currentlySelectedAxis != null && axis == currentlySelectedAxis) ? highlightOptions : normalOptions;

      for (double x = minX; x <= maxX; x += stepSize) {
        for (double y = minY; y <= maxY; y += stepSize) {
          for (double z = minZ; z <= maxZ; z += stepSize) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, usedOptions);
          }
        }
      }
    });
  }

  private void visualizeCuboidFor(Player player, Cuboid cuboid) {
    var stepSize = config.rootSection.mechanic.magnet.visualization.stepSize;

    var dustOptions = new Particle.DustOptions(
      config.rootSection.mechanic.magnet.visualization.visualizeColor._color,
      (float) config.rootSection.mechanic.magnet.visualization.dustSize
    );

    cuboid.forEachLine(false, (minX, minY, minZ, maxX, maxY, maxZ, axis) -> {
      for (double x = minX; x <= maxX; x += stepSize) {
        for (double y = minY; y <= maxY; y += stepSize) {
          for (double z = minZ; z <= maxZ; z += stepSize) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, dustOptions);
          }
        }
      }
    });
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public List<String> getDiscriminators() {
    return List.of("Magnet");
  }

  @EventHandler
  public void onSneakToggle(PlayerToggleSneakEvent event) {
    if (event.isSneaking())
      return;

    var session = editSessionByPlayerId.get(event.getPlayer().getUniqueId());

    if (session == null)
      return;

    displayHandler.show(event.getPlayer(), session);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var session = editSessionByPlayerId.get(event.getPlayer().getUniqueId());

    if (session == null || !session.clickDetection)
      return;

    var block = event.getClickedBlock();

    if (block != null && block.equals(session.sign.getBlock()))
      return;

    var action = event.getAction();

    if (action.isLeftClick())
      session.decreaseParameter();
    else
      session.increaseParameter();

    config.rootSection.mechanic.magnet.editModeClickDetectionActionbar.sendActionBar(session.player, session.makeEnvironment());

    event.setCancelled(true);
  }

  @Override
  public @Nullable MagnetInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.magnet")) {
      config.rootSection.mechanic.magnet.noPermission.sendMessage(creator);
      return null;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    if (!(mountBlock.getState() instanceof Container container)) {
      if (creator != null)
        config.rootSection.mechanic.magnet.noContainer.sendMessage(creator, environment);

      return null;
    }

    if (!config.rootSection.mechanic.magnet.allowMultipleSignsPerContainer) {
      if (SignUtil.checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
        if (creator != null)
          config.rootSection.mechanic.magnet.existingSign.sendMessage(creator, environment);

        return null;
      }
    }

    var parameters = new MagnetParameters(sign, config);

    parameters.read();

    var isDirty = parameters.writeIfDirty(false);
    var predicateAndLanguage = loadPredicateFromSign(sign);

    ItemPredicate predicate = null;

    var frontSide = sign.getSide(Side.FRONT);

    if (predicateAndLanguage != null) {
      if (!frontSide.line(0).equals(COMPONENT_PREDICATE_MODE_ON)) {
        frontSide.line(0, COMPONENT_PREDICATE_MODE_ON);
        isDirty = true;
      }

      predicate = predicateAndLanguage.predicate;
    }

    else {
      if (!frontSide.line(0).equals(COMPONENT_PREDICATE_MODE_OFF)) {
        frontSide.line(0, COMPONENT_PREDICATE_MODE_OFF);
        isDirty = true;
      }
    }

    if (isDirty) {
      sign.update(true, false);
      sign = (Sign) sign.getBlock().getState();
    }

    var cuboid = parameters.makeCuboid();

    var instance = new MagnetInstance(sign, cuboid, predicate);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceByMountBlockPosition.put(mountBlock.getWorld(), mountBlock.getX(), mountBlock.getY(), mountBlock.getZ(), instance);
    instanceCuboidRegistry.register(instance);

    if (creator != null)
      config.rootSection.mechanic.magnet.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  public @Nullable MagnetInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      instanceCuboidRegistry.unregister(instance);
      var mountBlock = instance.getMountBlock();
      instanceByMountBlockPosition.invalidate(mountBlock.getWorld(), mountBlock.getX(), mountBlock.getY(), mountBlock.getZ());
    }

    return instance;
  }

  public void visualizeInstance(Player player, MagnetInstance instance, boolean displayMessage) {
    var sign = instance.getSign();
    var visualizations = visualizationsByPlayerId.computeIfAbsent(player.getUniqueId(), k -> new VisualizationsBucket(player, config));

    visualizations.add(instance, lastTime);

    if (!displayMessage)
      return;

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ())
      .withVariable("visualization_duration", config.rootSection.mechanic.magnet.visualization.durationMs / 1000);

    config.rootSection.mechanic.magnet.visualizationInitialized.sendMessage(player, environment);
  }

  @Override
  public boolean onInstanceClick(Player player, MagnetInstance instance, boolean wasLeftClick) {
    var sign = instance.getSign();

    if (!canEditSign(player, sign))
      return true;

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    var playerId = player.getUniqueId();
    var existingSession = editSessionByPlayerId.get(playerId);

    if (existingSession != null && existingSession.sign.getBlock().equals(sign.getBlock())) {
      config.rootSection.mechanic.magnet.signClickedInEditMode.sendMessage(player, environment);
      return true;
    }

    if (!player.isSneaking())
      return false;

    if (wasLeftClick) {
      visualizeInstance(player, instance, true);
      return true;
    }

    if (existingSession != null) {
      config.rootSection.mechanic.magnet.alreadyInAnEditSession.sendMessage(player, environment);
      return true;
    }

    var parameters = new MagnetParameters(sign, config);
    parameters.read();

    editSessionByPlayerId.put(playerId, new EditSession(
      player, parameters,
      didWrite -> {
        editSessionByPlayerId.remove(playerId);

        if (didWrite) {
          reloadInstanceBySign(instance.getSign());
          config.rootSection.mechanic.magnet.editModeSaved.sendMessage(player, environment);
          return;
        }

        config.rootSection.mechanic.magnet.editModeSavedNoChanges.sendMessage(player, environment);
      },
      () -> {
        // Seeing how we're also using this branch if the mechanic is destroyed; make sure the UI is getting closed also.
        displayHandler.close(player);
        config.rootSection.mechanic.magnet.editModeCancelled.sendMessage(player, environment);
      }
    ));

    config.rootSection.mechanic.magnet.editModeInitialized.sendMessage(player, environment);
    return true;
  }

  @EventHandler(ignoreCancelled = true)
  public void onItemSpawn(ItemSpawnEvent event) {
    var itemEntity = event.getEntity();

    var itemStack = itemEntity.getItemStack();

    if (itemStack.getAmount() <= 0)
      return;

    var itemLocation = itemEntity.getLocation();

    var candidates = instanceCuboidRegistry.findCandidates(itemLocation);

    if (candidates.isEmpty())
      return;

    var targetMagnet = instanceCuboidRegistry.lookupClosest(candidates, itemLocation, candidate -> candidate.acceptsItem(itemStack));

    if (targetMagnet == null)
      return;

    event.setCancelled(true);

    // Seemingly, waiting until next tick is of utmost importance, because race-conditions can
    // occur otherwise, leading to a loss of items. Example: use a dispenser with an attached
    // comparator-clock and set the region in a way that it will automatically collect what has
    // been dropped by the block; the spawn-event is called in-between dropping the item and thus
    // calling this event and decrementing the slot - so if we add it immediately, we end up with
    // a net-zero, thereby a loss of the item at hand.
    Bukkit.getScheduler().runTaskLater(plugin, () -> targetMagnet.addItem(itemStack), 1);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    visualizationsByPlayerId.remove(playerId);
    editSessionByPlayerId.remove(playerId);
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    var instance = instanceByMountBlockPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ());

    if (instance != null)
      return instance.getSign();

    return null;
  }
}

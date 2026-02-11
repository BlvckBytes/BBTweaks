package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.MVisualizeCommand;
import me.blvckbytes.bbtweaks.mechanic.magnet.edit_display.EditDisplayHandler;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicRegistry;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
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

public class MagnetMechanic extends BaseMechanic<MagnetInstance> implements Listener {

  // TODO: Hide visualization of A if editing A
  // TODO: Select param in the UI with any button
  // TODO: Stop visualization on destroy

  private final CuboidMechanicRegistry<MagnetInstance> instanceCuboidRegistry;
  private final Map<UUID, VisualizationsBucket> visualizationsByPlayerId;
  private final Map<UUID, EditSession> editSessionByPlayerId;

  private final PredicateHelper predicateHelper;
  private final EditDisplayHandler displayHandler;

  public final NamespacedKey filterPredicateKey;
  public final NamespacedKey filterLanguageKey;

  private int lastTime;

  public MagnetMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    this.instanceCuboidRegistry = new CuboidMechanicRegistry<>();
    this.visualizationsByPlayerId = new HashMap<>();
    this.editSessionByPlayerId = new HashMap<>();

    ItemPredicateParserPlugin ipp;

    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
      throw new IllegalArgumentException("Expected plugin ItemPredicateParser to have been loaded at this point");

    this.predicateHelper = ipp.getPredicateHelper();
    this.displayHandler = new EditDisplayHandler(predicateHelper, FloodgateIntegration.load(plugin.getLogger()), config, plugin);

    var defaultLanguageCommand = Objects.requireNonNull(plugin.getCommand("mfilter"));
    var customLanguageCommand = Objects.requireNonNull(plugin.getCommand("mfilterl"));

    var filterCommandExecutor = new MFilterCommand(
      defaultLanguageCommand,
      customLanguageCommand,
      this,
      displayHandler,
      predicateHelper,
      config
    );

    defaultLanguageCommand.setExecutor(filterCommandExecutor);
    customLanguageCommand.setExecutor(filterCommandExecutor);

    var visualizeCommandExecutor = new MVisualizeCommand(this, config);

    Objects.requireNonNull(plugin.getCommand("mvisualize")).setExecutor(visualizeCommandExecutor);

    Bukkit.getServer().getPluginManager().registerEvents(displayHandler, plugin);

    this.filterPredicateKey = new NamespacedKey(plugin, "magnet-filter-predicate");
    this.filterLanguageKey = new NamespacedKey(plugin, "magnet-filter-language");
  }

  public List<MagnetInstance> getMagnetsInChunk(World world, int chunkX, int chunkZ) {
    return instanceCuboidRegistry.getWithinXZChunk(world, chunkX, chunkZ);
  }

  public @Nullable EditSession getEditSessionByPlayer(Player player) {
    return editSessionByPlayerId.get(player.getUniqueId());
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

    editSession.getCuboid().forEachLine((minX, minY, minZ, maxX, maxY, maxZ, axis) -> {
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

    cuboid.forEachLine((minX, minY, minZ, maxX, maxY, maxZ, axis) -> {
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

    var filter = PredicateAndLanguage.tryLoadFromSign(sign, filterPredicateKey, filterLanguageKey);

    var isDirty = PredicateAndLanguage.updatePredicateMarkerAndGetIfMadeChanges(sign, filter);

    parameters.read();
    isDirty |= parameters.writeIfDirty(false);

    if (isDirty) {
      sign.update(true, false);
      sign = (Sign) sign.getBlock().getState();
    }

    var cuboid = parameters.makeCuboid();

    var instance = new MagnetInstance(sign, cuboid, filter == null ? null : filter.predicate());

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceCuboidRegistry.register(instance);

    if (creator != null)
      config.rootSection.mechanic.magnet.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  public @Nullable MagnetInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    // TODO: There must be some sort of bug which doesn't properly unregister the instance here. I encountered
    //       old instances which no longer sucked items but were still visualized on the server today.
    if (instance != null)
      instanceCuboidRegistry.unregister(instance);

    return instance;
  }

  public void visualizeInstance(Player player, MagnetInstance instance, boolean displayMessage) {
    var sign = instance.getSign();
    var visualizations = visualizationsByPlayerId.computeIfAbsent(player.getUniqueId(), k -> new VisualizationsBucket(player, config));

    visualizations.add(instance, lastTime);

    if (!displayMessage)
      return;

    var signPredicate = PredicateAndLanguage.tryLoadFromSign(sign, filterPredicateKey, filterLanguageKey);

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    if (signPredicate != null) {
      var filterLanguageString = TranslationLanguage.matcher.getNormalizedName(signPredicate.language());
      var predicateString = PlainStringifier.stringify(signPredicate.predicate(), true);

      String command;

      if (predicateHelper.getSelectedLanguage(player) == signPredicate.language())
        command = "/mfilter " + predicateString;
      else
        command = "/mfilterl " + filterLanguageString + " " + predicateString;

      environment
        .withVariable("predicate_language", filterLanguageString)
        .withVariable("predicate", predicateString)
        .withVariable("filter_command", command);
    }

    config.rootSection.mechanic.magnet.visualizationInitialized.sendMessage(
      player,
      environment
        .withVariable("visualization_duration", config.rootSection.mechanic.magnet.visualization.durationMs / 1000)
    );
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

    var filter = PredicateAndLanguage.tryLoadFromSign(sign, filterPredicateKey, filterLanguageKey);

    editSessionByPlayerId.put(playerId, new EditSession(
      player, parameters, filter,
      filterPredicateKey, filterLanguageKey,
      didWrite -> {
        editSessionByPlayerId.remove(playerId);

        if (didWrite) {
          onSignUnload(instance.getSign());

          if (instance.getSign().getBlock().getState() instanceof Sign newSign)
            onSignLoad(newSign);

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
}

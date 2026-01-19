package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MagnetMechanic extends BaseMechanic<MagnetInstance> implements Listener {

  private final CuboidMechanicRegistry<MagnetInstance> instanceCuboidRegistry;
  private final Map<UUID, List<ShowSession>> showSessionsByPlayerId;
  private final Map<UUID, EditSession> editSessionByPlayerId;

  public MagnetMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    this.instanceCuboidRegistry = new CuboidMechanicRegistry<>();
    this.showSessionsByPlayerId = new HashMap<>();
    this.editSessionByPlayerId = new HashMap<>();
  }

  @Override
  public void onMechanicUnload() {
    super.onMechanicUnload();
    instanceCuboidRegistry.clear();
  }

  @Override
  public void tick(int time) {
    super.tick(time);

    if (time % config.rootSection.mechanic.magnet.visualization.periodTicks == 0) {
      showSessionsByPlayerId.values().forEach(this::handleVisualizations);
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

      // TODO: Expire edit-sessions if the player's XZ-distance to the sign exceeded a threshold
      if (visualization.isExpired() || !visualization.player.isOnline()) {
        visualizationIterator.remove();
        continue;
      }

      if (visualization instanceof EditSession editSession) {
        updateActionbarFor(visualization.player, editSession);
        visualizeCuboidFor(visualization.player, visualization.getCuboid(), true);
        continue;
      }

      visualizeCuboidFor(visualization.player, visualization.getCuboid(), false);
    }
  }

  private void updateActionbarFor(Player player, EditSession editSession) {
    var parameter = editSession.getCurrentParameter();
    player.sendActionBar(Component.text(parameter.name + " " + parameter.getValue()));
  }

  private void visualizeCuboidFor(Player player, Cuboid cuboid, boolean isEdit) {
    var stepSize = config.rootSection.mechanic.magnet.visualization.stepSize;

    var dustOptions = new Particle.DustOptions(
      isEdit
        ? config.rootSection.mechanic.magnet.visualization.editColor._color
        : config.rootSection.mechanic.magnet.visualization.visualizeColor._color,
      (float) config.rootSection.mechanic.magnet.visualization.dustSize
    );

    cuboid.forEachLine((minX, minY, minZ, maxX, maxY, maxZ) -> {
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
  public void onHotbarSlotChange(PlayerItemHeldEvent event) {
    var session = editSessionByPlayerId.get(event.getPlayer().getUniqueId());

    if (session == null)
      return;

    var isScrollingRight = (event.getNewSlot() - event.getPreviousSlot() + 9) % 9 == 1;

    if (isScrollingRight) {
      session.increaseParameter();
      return;
    }

    session.decreaseParameter();
  }

  @EventHandler
  public void onSneakToggle(PlayerToggleSneakEvent event) {
    if (!event.isSneaking())
      return;

    var session = editSessionByPlayerId.get(event.getPlayer().getUniqueId());

    // TODO: From a UX sandpoint, this *sucks*. Maybe a 9x1 UI would be better? Could also contain the save-button.
    if (session != null)
      session.nextParameter();
  }

  @Override
  public @Nullable MagnetInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.magnet")) {
      creator.sendMessage("§cNo permission!");
      return null;
    }

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    if (!(mountBlock.getState() instanceof Container)) {
      if (creator != null)
        creator.sendMessage("§cMagnets may only be mounted on containers!");

      return null;
    }

    var parameters = new MagnetParameters(sign, config);

    parameters.read();
    parameters.writeIfDirty();

    var cuboid = parameters.makeCuboid();

    // TODO: Allow to create and set filters, probably using ipp (still requires more brainstorming)

    var instance = new MagnetInstance(sign, cuboid, null);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceCuboidRegistry.register(instance);

    if (creator != null)
      creator.sendMessage("§aMagnet created!");

    return instance;
  }

  @Override
  public @Nullable MagnetInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null)
      instanceCuboidRegistry.unregister(instance);

    return instance;
  }

  @Override
  public boolean onInstanceClick(Player player, MagnetInstance instance, boolean wasLeftClick) {
    if (player.isSneaking()) {
      var playerId = player.getUniqueId();

      if (wasLeftClick) {
        var playerBucket = showSessionsByPlayerId.computeIfAbsent(playerId, k -> new ArrayList<>());
        playerBucket.removeIf(entry -> entry.getCuboid().doBoundsEqual(instance.getCuboid()));
        playerBucket.add(new ShowSession(player, instance.getCuboid(), config));
        player.sendMessage("§aVisualizing cuboid!");
        return true;
      }

      var existingSession = editSessionByPlayerId.remove(playerId);

      if (existingSession != null) {
        existingSession.parameters.writeIfDirty();
        onSignUnload(existingSession.parameters.sign);
        onSignLoad(existingSession.parameters.sign);
        player.sendMessage("§aFinished edit-session!");
        return true;
      }

      var parameters = new MagnetParameters(instance.getSign(), config);
      parameters.read();

      editSessionByPlayerId.put(playerId, new EditSession(player, parameters));
      player.sendMessage("§aStarted edit-session");
      return true;
    }

    return false;
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

    targetMagnet.addItem(itemStack);
    event.setCancelled(true);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    showSessionsByPlayerId.remove(playerId);
    editSessionByPlayerId.remove(playerId);
  }
}

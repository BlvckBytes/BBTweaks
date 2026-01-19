package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicRegistry;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.Particle;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MagnetMechanic extends BaseMechanic<MagnetInstance> implements Listener {

  private static final int EXTENTS_LINE_INDEX = 2;
  private static final int OFFSETS_LINE_INDEX = 3;

  private record VisualizeSession(Player player, Cuboid cuboid, long createdAt) {}

  private final CuboidMechanicRegistry<MagnetInstance> instanceCuboidRegistry;
  private final Map<UUID, List<VisualizeSession>> visualizationByPlayerId;

  public MagnetMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);

    this.instanceCuboidRegistry = new CuboidMechanicRegistry<>();
    this.visualizationByPlayerId = new HashMap<>();
  }

  @Override
  public void onMechanicUnload() {
    super.onMechanicUnload();
    instanceCuboidRegistry.clear();
  }

  @Override
  public void tick(int time) {
    super.tick(time);

    if (time % config.rootSection.mechanic.magnet.visualization.periodTicks == 0)
      handleVisualizations();

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

  private void handleVisualizations() {
    for (var playerBucket : visualizationByPlayerId.values()) {
      for (var visualizationIterator = playerBucket.iterator(); visualizationIterator.hasNext();) {
        var visualization = visualizationIterator.next();
        var age = System.currentTimeMillis() - visualization.createdAt;

        if (age >= config.rootSection.mechanic.magnet.visualization.durationMs || !visualization.player.isOnline()) {
          visualizationIterator.remove();
          continue;
        }

        visualizeCuboidFor(visualization.player, visualization.cuboid);
      }
    }
  }

  private void visualizeCuboidFor(Player player, Cuboid cuboid) {
    var stepSize = config.rootSection.mechanic.magnet.visualization.stepSize;

    var dustOptions = new Particle.DustOptions(
      config.rootSection.mechanic.magnet.visualization._color,
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

    var extentsTokens = getTokens(SignUtil.getPlainTextLine(sign, EXTENTS_LINE_INDEX));
    var offsetsTokens = getTokens(SignUtil.getPlainTextLine(sign, OFFSETS_LINE_INDEX));

    // TODO: Validate tokens and override sign-lines to sane defaults otherwise

    // TODO: Properly parameterize this cuboid based on the tokens above
    var cuboid = new Cuboid(
      signBlock.getRelative(-3, -3, -3),
      signBlock.getRelative(3, 3, 3)
    );

    // TODO: Allow to create and set filters, probably using ipp (still requires more brainstorming)

    var instance = new MagnetInstance(signBlock, signFacing, cuboid, null);

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
    if (!player.isSneaking() || !wasLeftClick)
      return false;

    var playerBucket = visualizationByPlayerId.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());

    playerBucket.removeIf(entry -> entry.cuboid.doBoundsEqual(instance.getCuboid()));
    playerBucket.add(new VisualizeSession(player, instance.getCuboid(), System.currentTimeMillis()));

    player.sendMessage("§aVisualizing cuboid!");

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

    targetMagnet.addItem(itemStack);
    event.setCancelled(true);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    visualizationByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private List<String> getTokens(String input) {
    var result = new ArrayList<String>();
    var tokenBeginIndex = -1;

    for (var charIndex = 0; charIndex < input.length(); ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var isWhitespace = Character.isWhitespace(currentChar);

      if (isWhitespace) {
        if (tokenBeginIndex < 0)
          continue;

        result.add(input.substring(tokenBeginIndex, charIndex));
        tokenBeginIndex = -1;
        continue;
      }

      if (tokenBeginIndex < 0)
        tokenBeginIndex = charIndex;
    }

    if (tokenBeginIndex >= 0)
      result.add(input.substring(tokenBeginIndex));

    return result;
  }
}

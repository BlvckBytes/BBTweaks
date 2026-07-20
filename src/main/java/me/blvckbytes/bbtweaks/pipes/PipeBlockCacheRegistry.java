package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PipeBlockCacheRegistry implements Listener, Disableable, Tickable {

  private static final int EXPIRED_TICKET_REMOVAL_INTERVAL_T = 5;

  static {
    CachedBlock.setupPresetTable();
  }

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, PipeBlockCache> blockCacheByWorldUid;

  private long relativeTimeTicks;

  public PipeBlockCacheRegistry(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.config = config;

    this.blockCacheByWorldUid = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTimeTicks = relativeTime;

    if (relativeTime % EXPIRED_TICKET_REMOVAL_INTERVAL_T == 0) {
      for (var cache : blockCacheByWorldUid.values())
        cache.expireChunkTickets(false);
    }
  }

  public long getRelativeTimeTicks() {
    return relativeTimeTicks;
  }

  public PipeBlockCache getBlockCache(World world) {
    return blockCacheByWorldUid.computeIfAbsent(world.getUID(), _ -> new PipeBlockCache(plugin, config, world, this));
  }

  @Override
  public void disable() {
    for (var cache : blockCacheByWorldUid.values())
      cache.disable();

    blockCacheByWorldUid.clear();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    invalidateCache(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    invalidateCache(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonExtend(BlockPistonExtendEvent event) {
    for (var block : event.getBlocks()) {
      invalidateCache(block);
      invalidateCache(block.getRelative(event.getDirection()));
    }
    invalidateCache(event.getBlock());
    invalidateCache(event.getBlock().getRelative(event.getDirection()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPistonRetract(BlockPistonRetractEvent event) {
    for (var block : event.getBlocks()) {
      invalidateCache(block);
      invalidateCache(block.getRelative(event.getDirection()));
    }
    invalidateCache(event.getBlock());
    invalidateCache(event.getBlock().getRelative(event.getDirection()));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    for (var block : event.blockList())
      invalidateCache(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    for (var block : event.blockList())
      invalidateCache(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSignChange(SignChangeEvent event) {
    invalidateCache(event.getBlock());
  }

  @EventHandler
  public void onCacheInvalidation(InvalidateCachedBlockEvent event) {
    invalidateCache(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPhysics(BlockPhysicsEvent event) {
    var block = event.getBlock();
    var type = block.getType();

    Block supportingBlock;

    if (Tag.STANDING_SIGNS.isTagged(type))
      supportingBlock = block.getRelative(BlockFace.DOWN);
    else if (Tag.WALL_SIGNS.isTagged(type))
      supportingBlock = block.getRelative(((WallSign) block.getBlockData()).getFacing().getOppositeFace());
    else
      return;

    if (supportingBlock.getType() == Material.AIR)
      invalidateCache(block);
  }

  private void invalidateCache(Block block) {
    var blockCache = blockCacheByWorldUid.get(block.getWorld().getUID());

    if (blockCache != null)
      blockCache.invalidateCache(block);
  }
}

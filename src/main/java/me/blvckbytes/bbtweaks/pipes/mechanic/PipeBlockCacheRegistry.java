package me.blvckbytes.bbtweaks.pipes.mechanic;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PipeBlockCacheRegistry implements Listener, Disableable {

  private static final int EXPIRED_TICKET_REMOVAL_INTERVAL_T = 5;

  static {
    CachedBlock.setupPresetTable();
  }

  private final Plugin plugin;
  private final Map<UUID, PipeBlockCache> blockCacheByWorldUid;

  private final BukkitTask chunkTicketTask;

  private int relativeTimeTicks;

  public PipeBlockCacheRegistry(
    Plugin plugin
  ) {
    this.plugin = plugin;
    this.blockCacheByWorldUid = new HashMap<>();

    this.chunkTicketTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      relativeTimeTicks += EXPIRED_TICKET_REMOVAL_INTERVAL_T;

      for (var cache : blockCacheByWorldUid.values())
        cache.expireChunkTickets(false);
    }, 0, EXPIRED_TICKET_REMOVAL_INTERVAL_T);
  }

  public int getRelativeTimeTicks() {
    return relativeTimeTicks;
  }

  public PipeBlockCache getBlockCache(World world) {
    return blockCacheByWorldUid.computeIfAbsent(world.getUID(), _ -> new PipeBlockCache(plugin, world, this));
  }

  @Override
  public void disable() {
    HandlerList.unregisterAll(this);
    this.chunkTicketTask.cancel();

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

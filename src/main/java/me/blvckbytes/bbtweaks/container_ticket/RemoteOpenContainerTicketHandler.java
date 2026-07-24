package me.blvckbytes.bbtweaks.container_ticket;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.CompactId;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class RemoteOpenContainerTicketHandler implements Listener, Tickable {

  private record BlockDataCapture(Block block, BlockData capturedData) {
    BlockDataCapture(Block block) {
      this(block, block.getBlockData());
    }
  }

  private record InventoryAndBlocks(Inventory inventory, List<BlockDataCapture> dataCaptures) {
    boolean didBlocksChange() {
      for (var dataCapture : dataCaptures) {
        var currentData = dataCapture.block.getBlockData();

        if (!dataCapture.capturedData.equals(currentData))
          return true;
      }

      return false;
    }
  }

  private record ChunkBucket(
    long chunkId,
    MutableInt viewedBlockCount,
    List<InventoryAndBlocks> viewedInventories
  ) {
    ChunkBucket(long chunkId) {
      this(chunkId, new MutableInt(0), new ArrayList<>());
    }

    boolean hasInventory(Inventory inventory) {
      for (var viewedInventory : viewedInventories) {
        if (viewedInventory.inventory.equals(inventory))
          return true;
      }

      return false;
    }

    void addViewedInventory(Inventory inventory) {
      if (hasInventory(inventory))
        return;

      var dataCaptures = new ArrayList<BlockDataCapture>();

      for (var block : BlockUtil.getInventoryBackingBlocks(inventory))
        dataCaptures.add(new BlockDataCapture(block));

      viewedInventories.add(new InventoryAndBlocks(inventory, dataCaptures));
    }
  }

  private final Plugin plugin;

  private final Map<UUID, Long2ObjectMap<ChunkBucket>> chunkBucketByChunkIdByWorldId;

  public RemoteOpenContainerTicketHandler(Plugin plugin) {
    this.plugin = plugin;

    this.chunkBucketByChunkIdByWorldId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    var changedInstances = new HashSet<InventoryAndBlocks>();

    for (var worldBucket : chunkBucketByChunkIdByWorldId.values()) {
      for (var chunkBucket : worldBucket.values()) {
        for (var inventoryIterator = chunkBucket.viewedInventories.iterator(); inventoryIterator.hasNext();) {
          var viewedInventory = inventoryIterator.next();

          if (!viewedInventory.didBlocksChange())
            continue;

          inventoryIterator.remove();
          changedInstances.add(viewedInventory);
        }
      }
    }

    for (var inventoryAndBlocks : changedInstances) {
      // We need to decrement the view-counters here because later on, in the corresponding
      // close-event, the backing-block that has been changed will no longer show up, so
      // we would leak a view-count for it; thus, decrement for every captured block manually.
      for (var dataCapture : inventoryAndBlocks.dataCaptures)
        modifyBlockViewCounter(dataCapture.block, inventoryAndBlocks.inventory, false, true);

      for (var viewer : new ArrayList<>(inventoryAndBlocks.inventory.getViewers()))
        viewer.closeInventory();
    }
  }

  @EventHandler
  public void preRemoteContainerOpen(PreRemoteContainerOpenEvent event) {
    var worldBucket = accessWorldBucket(event.containerBlock);
    var backingBlocks = BlockUtil.getInventoryBackingBlocks(event.containerInventory);

    for (var block : backingBlocks) {
      var chunkBucket = tryAccessChunkBucket(worldBucket, block, true);

      assert chunkBucket != null;

      chunkBucket.addViewedInventory(event.containerInventory);
    }
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent event) {
    for (var block : BlockUtil.getInventoryBackingBlocks(event.getInventory()))
      modifyBlockViewCounter(block, event.getInventory(), true, false);
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    for (var block : BlockUtil.getInventoryBackingBlocks(event.getInventory()))
      modifyBlockViewCounter(block, event.getInventory(), false, false);
  }

  private void modifyBlockViewCounter(Block containerBlock, Inventory inventory, boolean increment, boolean ignoreHasInventory) {
    var worldBucket = accessWorldBucket(containerBlock);
    var chunkBucket = tryAccessChunkBucket(worldBucket, containerBlock, false);

    // Only act if a prior remote-access event initialized the bucket beforehand.
    if (chunkBucket == null)
      return;

    if (!ignoreHasInventory && !chunkBucket.hasInventory(inventory))
      return;

    if (increment) {
      if (++chunkBucket.viewedBlockCount.value == 1) {
        if (!containerBlock.getChunk().addPluginChunkTicket(plugin))
          plugin.getLogger().log(Level.WARNING, "Could not add chunk-ticket for block at " + containerBlock.getLocation());
      }

      return;
    }

    if (--chunkBucket.viewedBlockCount.value > 0)
      return;

    // Remove the count, as to thereby release this chunk from being marked
    // as holding a container that's accessed remotely.
    worldBucket.remove(chunkBucket.chunkId);

    if (!containerBlock.getChunk().removePluginChunkTicket(plugin))
      plugin.getLogger().log(Level.WARNING, "Could not remove chunk-ticket for block at " + containerBlock.getLocation());
  }

  private Long2ObjectMap<ChunkBucket> accessWorldBucket(Block block) {
    return chunkBucketByChunkIdByWorldId
      .computeIfAbsent(block.getWorld().getUID(), _ -> new Long2ObjectArrayMap<>());
  }

  private @Nullable ChunkBucket tryAccessChunkBucket(Long2ObjectMap<ChunkBucket> worldBucket, Block block, boolean create) {
    var chunkId = CompactId.computeWorldlessChunkId(block.getX() >> 4, block.getZ() >> 4);

    if (create)
      return worldBucket.computeIfAbsent(chunkId, ChunkBucket::new);

    return worldBucket.get(chunkId);
  }
}

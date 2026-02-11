package me.blvckbytes.bbtweaks.mechanic.magnet;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.mechanic.util.ComparableMap;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.mechanic.util.PositionOfBlock;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class FakeBlocksVisualization {

  private final Player player;
  private final MagnetInstance magnetInstance;
  public final int createdAt;
  private final ComparableMap<PositionOfBlock, BlockData> fakeBlocks;
  private final BlockData fakeBlockData;

  public FakeBlocksVisualization(Player player, MagnetInstance magnetInstance, int createdAt) {
    this.player = player;
    this.magnetInstance = magnetInstance;
    this.createdAt = createdAt;
    this.fakeBlocks = new ComparableMap<>(PositionOfBlock.class);
    this.fakeBlockData = Material.BLUE_STAINED_GLASS.createBlockData();
  }

  public void update() {
    var cuboid = magnetInstance.getCuboid();
    var world = player.getWorld();

    var accessibleChunks = new Long2ObjectOpenHashMap<Chunk>();

    cuboid.forEachXZChunk((chunkX, chunkZ) -> {
      if (world.isChunkLoaded(chunkX, chunkZ))
        accessibleChunks.put(IntTuple.create(chunkX, chunkZ), world.getChunkAt(chunkX, chunkZ));
    });

    var outdatedKeys = new HashSet<>(fakeBlocks.keySet());
    var newBlocksCount = new MutableInt();

    // TODO: If the blocks should mark the cuboid inclusively, we must subtract 1 from each axis' max, I believe.

    cuboid.forEachLine((minX, minY, minZ, maxX, maxY, maxZ, axis) -> {
      for (var x = minX; x <= maxX; ++x) {
        for (var y = minY; y <= maxY; ++y) {
          for (var z = minZ; z <= maxZ; ++z) {
            var chunkX = x >> 4;
            var chunkZ = z >> 4;
            var chunk = accessibleChunks.get(IntTuple.create(chunkX, chunkZ));

            if (chunk == null)
              continue;

            var lineBlock = chunk.getBlock(x & 0xF, y, z & 0xF);
            var position = new PositionOfBlock(lineBlock);

            if (outdatedKeys.remove(position))
              continue;

            fakeBlocks.put(position, fakeBlockData);
            ++newBlocksCount.value;
          }
        }
      }
    });

    var hadOutdatedKeys = !outdatedKeys.isEmpty();

    if (hadOutdatedKeys) {
      // TODO: IMPORTANT! Only get block-data if we're inside an accessible chunk (to not load unloaded chunks)!
      //       Also, cache results for the duration of this visualization.
      for (var remainingKey : outdatedKeys)
        fakeBlocks.put(remainingKey, remainingKey.block.getBlockData());
    }

    if (newBlocksCount.value > 0 || hadOutdatedKeys) {
      //noinspection UnstableApiUsage
      player.sendMultiBlockChange(fakeBlocks);
    }

    if (hadOutdatedKeys) {
      for (var remainingKey : outdatedKeys)
        fakeBlocks.remove(remainingKey);
    }
  }

  public void undo() {
    for (var entry : fakeBlocks.entrySet())
      entry.setValue(entry.getKey().block.getBlockData());

    //noinspection UnstableApiUsage
    player.sendMultiBlockChange(fakeBlocks);

    fakeBlocks.clear();
  }
}

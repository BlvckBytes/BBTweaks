package me.blvckbytes.bbtweaks.locate_entities;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record ChunkCluster(
  int totalCount,
  List<Long> chunks,
  long highestCountChunk
) {

  private static final BlockFace[] NEIGHBOR_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  public static List<ChunkCluster> findClusters(Long2IntOpenHashMap counts) {
    var remaining = new Long2IntOpenHashMap(counts);
    var clusters = new ArrayList<ChunkCluster>();

    while (!remaining.isEmpty()) {
      var start = remaining.keySet().iterator().nextLong();

      var queue = new LongArrayFIFOQueue();
      var chunks = new ArrayList<Long>();

      int totalCount = 0;
      int highestCount;

      var highestCountChunk = start;

      var startCount = remaining.remove(start);

      queue.enqueue(start);
      chunks.add(start);

      totalCount += startCount;
      highestCount = startCount;

      while (!queue.isEmpty()) {
        var current = queue.dequeueLong();

        var x = IntTuple.getFirst(current);
        var z = IntTuple.getSecond(current);

        for (var offset : NEIGHBOR_FACES) {
          var neighbor = IntTuple.create(x + offset.getModX(), z + offset.getModZ());

          if (!remaining.containsKey(neighbor))
            continue;

          var count = remaining.remove(neighbor);

          queue.enqueue(neighbor);
          chunks.add(neighbor);
          totalCount += count;

          if (count > highestCount) {
            highestCount = count;
            highestCountChunk = neighbor;
          }
        }
      }

      clusters.add(new ChunkCluster(
        totalCount,
        chunks,
        highestCountChunk
      ));
    }

    clusters.sort(Comparator.comparingInt(ChunkCluster::totalCount).reversed());

    return clusters;
  }
}

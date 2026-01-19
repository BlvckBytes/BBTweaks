package me.blvckbytes.bbtweaks.mechanic.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class CuboidMechanicRegistry<InstanceType extends CuboidMechanicInstance> {

  private record WorldBucket<T>(
    Long2ObjectMap<List<T>> instanceByXYZChunkId,
    Long2ObjectMap<List<T>> instanceByXZTuple
  ) {
    WorldBucket() {
      this(new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>());
    }
  }

  private final Map<UUID, WorldBucket<InstanceType>> instanceByChunkIdByWorldId;

  public CuboidMechanicRegistry() {
    this.instanceByChunkIdByWorldId = new HashMap<>();
  }

  public void forEachXZChunkBucket(XZChunkBucketHandler<InstanceType> chunkBucketHandler) {
    for (var worldBucketEntry : instanceByChunkIdByWorldId.entrySet()) {
      var world = Bukkit.getWorld(worldBucketEntry.getKey());

      if (world == null)
        continue;

      var worldBucket = worldBucketEntry.getValue();

      for (var entry : worldBucket.instanceByXZTuple.long2ObjectEntrySet()) {
        var entries = entry.getValue();

        if (entries.isEmpty())
          continue;

        var xzTuple = entry.getLongKey();
        var chunkX = IntTuple.getFirst(xzTuple);
        var chunkZ = IntTuple.getSecond(xzTuple);

        chunkBucketHandler.handle(world, chunkX, chunkZ, entries);
      }
    }
  }

  public void register(InstanceType instance) {
    var worldId = instance.getSignBlock().getWorld().getUID();
    var worldBucket = instanceByChunkIdByWorldId.computeIfAbsent(worldId, k -> new WorldBucket<>());

    instance.getCuboid().forEachXYZChunk((chunkX, chunkY, chunkZ) -> {
      var xyzChunkId = computeXYZChunkId(chunkX, chunkY, chunkZ);
      var chunkBucket = worldBucket.instanceByXYZChunkId.computeIfAbsent(xyzChunkId, k -> new ArrayList<>());
      chunkBucket.add(instance);
    });

    instance.getCuboid().forEachXZChunk((chunkX, chunkZ) -> {
      var xzTuple = IntTuple.create(chunkX, chunkZ);
      var chunkBucket = worldBucket.instanceByXZTuple.computeIfAbsent(xzTuple, k -> new ArrayList<>());
      chunkBucket.add(instance);
    });
  }

  public void unregister(InstanceType instance) {
    var worldId = instance.getSignBlock().getWorld().getUID();
    var worldBucket = instanceByChunkIdByWorldId.get(worldId);

    if (worldBucket == null)
      return;

    instance.getCuboid().forEachXYZChunk((chunkX, chunkY, chunkZ) -> {
      var xyzChunkId = computeXYZChunkId(chunkX, chunkY, chunkZ);
      var chunkBucket = worldBucket.instanceByXYZChunkId.get(xyzChunkId);

      if (chunkBucket != null)
        chunkBucket.removeIf(entry -> entry.getSignBlock().equals(instance.getSignBlock()));
    });

    instance.getCuboid().forEachXZChunk((chunkX, chunkZ) -> {
      var xzTuple = IntTuple.create(chunkX, chunkZ);
      var chunkBucket = worldBucket.instanceByXZTuple.get(xzTuple);

      if (chunkBucket != null)
        chunkBucket.removeIf(entry -> entry.getSignBlock().equals(instance.getSignBlock()));
    });
  }

  public void clear() {
    instanceByChunkIdByWorldId.clear();
  }

  public @Nullable InstanceType lookupClosest(List<InstanceType> candidates, Location location, Predicate<InstanceType> predicate) {
    InstanceType minDistanceInstance = null;
    var minDistanceSquared = 0;

    for (var candidate : candidates) {
      if (!candidate.getCuboid().isPointInside(location))
        continue;

      if (predicate != null && !predicate.test(candidate))
        continue;

      var distanceSquared = distanceSquared(candidate.getSignBlock(), location);

      if (minDistanceInstance == null || distanceSquared < minDistanceSquared) {
        minDistanceSquared = distanceSquared;
        minDistanceInstance = candidate;
      }
    }

    return minDistanceInstance;
  }

  public List<InstanceType> findCandidates(Location location) {
    var world = location.getWorld();

    if (world == null)
      return Collections.emptyList();

    var worldBucket = instanceByChunkIdByWorldId.get(world.getUID());

    if (worldBucket == null)
      return Collections.emptyList();

    var xyzChunkId = computeXYZChunkId(location.getBlockX() >> 4, location.getBlockY() >> 4, location.getBlockZ() >> 4);
    var chunkBucket = worldBucket.instanceByXYZChunkId.get(xyzChunkId);

    if (chunkBucket == null)
      return Collections.emptyList();

    List<InstanceType> result = null;

    for (var candidate : chunkBucket) {
      if (!candidate.getCuboid().isPointInside(location))
        continue;

      if (result == null)
        result = new ArrayList<>();

      result.add(candidate);
    }

    return result == null ? Collections.emptyList() : result;
  }

  private int distanceSquared(Block block, Location location) {
    // At least for now, this will operate on integers, as I see no reason for double-precision.
    var deltaX = block.getX() - location.getBlockX();
    var deltaY = block.getY() - location.getBlockY();
    var deltaZ = block.getZ() - location.getBlockZ();
    return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
  }

  private static long computeXYZChunkId(int chunkX, int chunkY, int chunkZ) {
    // x/z in [-30M;30M] => chunk x/z in [-1.875M;+1.875M], adding 1.875M will result in [0;3.75M], 22 bits
    // y in [-64;320] => chunk y in [-4;20], adding 4 will result in [0;24], 5 bits

    // <15b unused><22b chunk_x><5b chunk_y><22b chunk_z>

    // 2^22 - 1 = 0x3FFFFF
    // 2^5 - 1 = 0x1F
    return (
      (((chunkX + 1_875_000L) & 0x3FFFFF) << 27)
        | (((chunkY + 4) & 0x1F) << 22)
        | ((chunkZ + 1_875_000L) & 0x3FFFFF)
    );
  }
}

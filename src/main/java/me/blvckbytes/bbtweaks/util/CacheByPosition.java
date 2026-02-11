package me.blvckbytes.bbtweaks.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CacheByPosition<T> {

  private final Map<UUID, Long2ObjectMap<T>> cachedItemByFastHashByWorldId;

  // I'm going to be completely honest: at the current point in time, it's simply too much of a hassle
  // to coordinate iteration/mutation on the foreach-helpers, so let's buffer them instead of running
  // into issues with an iterator; for the sake of performance, the buffer is reused, as we're always
  // on the main-thread (as enforced by the buffered-iteration helper-method).
  private @Nullable List<T> valuesBuffer;

  public CacheByPosition() {
    this.cachedItemByFastHashByWorldId = new HashMap<>();
  }

  public void forEachValue(Consumer<T> handler) {
    handleBufferedIteration(buffer -> {
      for (var bucket : cachedItemByFastHashByWorldId.values())
        buffer.addAll(bucket.values());
    }, handler);
  }

  public void forEachValue(World world, Consumer<T> handler) {
    var bucket = cachedItemByFastHashByWorldId.get(world.getUID());

    if (bucket != null)
      handleBufferedIteration(buffer -> buffer.addAll(bucket.values()), handler);
  }

  private void handleBufferedIteration(Consumer<List<T>> bufferLoader, Consumer<T> iterationHandler) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalArgumentException("Only support iterating on the main thread");

    if (valuesBuffer == null)
      valuesBuffer = new ArrayList<>();
    else
      valuesBuffer.clear();

    bufferLoader.accept(valuesBuffer);

    for (var value : valuesBuffer)
      iterationHandler.accept(value);
  }

  public void clear() {
    this.cachedItemByFastHashByWorldId.clear();
  }

  public T put(World world, int x, int y, int z, T value) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.computeIfAbsent(worldId, k -> new Long2ObjectOpenHashMap<>());
    var blockId = computeWorldlessBlockId(x, y, z);
    return worldBucket.put(blockId, value);
  }

  public @Nullable T get(World world, int x, int y, int z) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.get(worldId);

    if (worldBucket == null)
      return null;

    var blockId = computeWorldlessBlockId(x, y, z);

    return worldBucket.get(blockId);
  }

  public T computeIfAbsent(World world, int x, int y, int z, Supplier<T> computeFunction) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.computeIfAbsent(worldId, k -> new Long2ObjectOpenHashMap<>());
    var blockId = computeWorldlessBlockId(x, y, z);
    return worldBucket.computeIfAbsent(blockId, k -> computeFunction.get());
  }

  public @Nullable T invalidate(World world, int x, int y, int z) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.get(worldId);

    if (worldBucket == null)
      return null;

    return worldBucket.remove(computeWorldlessBlockId(x, y, z));
  }

  public static long computeWorldlessBlockId(int x, int y, int z) {
    // y in [-64;320], adding 64 will result in [0;384], 9 bits
    // x/z in [-30M;30M], adding 30M will result in [0;60M], 26 bits

    // <3b unused><26b z><26b x><9b y>

    return (
      // 2^9 - 1 = 0x1FF
      // 2^26 - 1 = 0x3FFFFFF
      ((y + 64) & 0x1FF)
        | (((x + 30_000_000L) & 0x3FFFFFF) << 9)
        | (((z + 30_000_000L) & 0x3FFFFFF) << (9 + 26))
    );
  }
}

package me.blvckbytes.bbtweaks.furnace_level_display;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class CacheByPosition<T> {

  private final Map<UUID, Long2ObjectMap<T>> cachedItemByFastHashByWorldId;

  public CacheByPosition() {
    this.cachedItemByFastHashByWorldId = new HashMap<>();
  }

  public T computeIfAbsent(World world, int x, int y, int z, Supplier<T> computeFunction) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.computeIfAbsent(worldId, k -> new Long2ObjectOpenHashMap<>());
    var blockId = computeWorldlessBlockId(x, y, z);
    return worldBucket.computeIfAbsent(blockId, k -> computeFunction.get());
  }

  public void invalidate(World world, int x, int y, int z) {
    var worldId = world.getUID();
    var worldBucket = cachedItemByFastHashByWorldId.get(worldId);

    if (worldBucket == null)
      return;

    worldBucket.remove(computeWorldlessBlockId(x, y, z));
  }

  public static long computeWorldlessBlockId(int x, int y, int z) {
    // y in [-64;320], adding 64 will result in [0;384], 9 bits
    // x/z in [-30M;30M], adding 30M will result in [0;60M], 26 bits

    // <3b unused><26b z><26b x><9b y>

    return (
      // 2^9 - 1 = 0x1FF
      // 2^26 - 1 = 0x3FFFFFF
      // 2^3 - 1 = 0x7
      ((y + 64) & 0x1FF)
        | (((x + 30_000_000L) & 0x3FFFFFF) << 9)
        | (((z + 30_000_000L) & 0x3FFFFFF) << (9 + 26))
    );
  }
}

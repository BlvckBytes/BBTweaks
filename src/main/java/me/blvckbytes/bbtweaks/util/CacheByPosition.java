package me.blvckbytes.bbtweaks.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CacheByPosition<T> {

  private final Map<UUID, Long2ObjectMap<T>> cachedItemByFastHashByWorldId;

  public CacheByPosition() {
    this.cachedItemByFastHashByWorldId = new HashMap<>();
  }

  public void forEachValue(IterationHandler<T> handler) {
    /*
      TODO: Investigate this error
      [19:40:24 WARN]: [BBTweaks] Task #92 for BBTweaks v0.1 generated an exception
      java.lang.NullPointerException: Cannot invoke "it.unimi.dsi.fastutil.longs.LongArrayList.getLong(int)" because "this.wrapped" is null
        at it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$MapIterator.nextEntry(Long2ObjectOpenHashMap.java:676) ~[fastutil-8.5.18.jar:?]
        at it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap$ValueIterator.next(Long2ObjectOpenHashMap.java:1185) ~[fastutil-8.5.18.jar:?]
        at BBTweaks-0.1.jar//me.blvckbytes.bbtweaks.util.CacheByPosition.forEachValue(CacheByPosition.java:25) ~[?:?]
        at BBTweaks-0.1.jar//me.blvckbytes.bbtweaks.mechanic.BaseMechanic.tick(BaseMechanic.java:87) ~[?:?]
        at BBTweaks-0.1.jar//me.blvckbytes.bbtweaks.mechanic.SignMechanicManager.lambda$tick$0(SignMechanicManager.java:91) ~[?:?]
        at java.base/java.util.HashMap$Values.forEach(HashMap.java:1073) ~[?:?]
        at BBTweaks-0.1.jar//me.blvckbytes.bbtweaks.mechanic.SignMechanicManager.tick(SignMechanicManager.java:91) ~[?:?]
        at org.bukkit.craftbukkit.scheduler.CraftTask.run(CraftTask.java:78) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at org.bukkit.craftbukkit.scheduler.CraftScheduler.mainThreadHeartbeat(CraftScheduler.java:474) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.MinecraftServer.tickChildren(MinecraftServer.java:1761) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.MinecraftServer.tickServer(MinecraftServer.java:1616) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.dedicated.DedicatedServer.tickServer(DedicatedServer.java:427) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.MinecraftServer.processPacketsAndTick(MinecraftServer.java:1672) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1340) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at net.minecraft.server.MinecraftServer.lambda$spin$2(MinecraftServer.java:388) ~[paper-1.21.11.jar:1.21.11-111-302c47c]
        at java.base/java.lang.Thread.run(Thread.java:1583) ~[?:?]
     */
    for (var bucket : cachedItemByFastHashByWorldId.values()) {
      for (var itemIterator = bucket.values().iterator(); itemIterator.hasNext();) {
        var decision = handler.handle(itemIterator.next());

        if (decision == IterationDecision.REMOVE_AND_CONTINUE || decision == IterationDecision.REMOVE_AND_BREAK)
          itemIterator.remove();

        if (decision == IterationDecision.BREAK || decision == IterationDecision.REMOVE_AND_BREAK)
          return;
      }
    }
  }

  public void forEachValue(World world, Consumer<T> handler) {
    var bucket = cachedItemByFastHashByWorldId.get(world.getUID());

    if (bucket != null)
      bucket.values().forEach(handler);
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

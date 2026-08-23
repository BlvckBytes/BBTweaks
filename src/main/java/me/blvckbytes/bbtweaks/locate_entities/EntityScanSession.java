package me.blvckbytes.bbtweaks.locate_entities;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class EntityScanSession {

  private static final int MILLISECONDS_PER_TICK = 5;

  public final World world;
  public final @Nullable EntityType targetEntityType;

  private final Chunk[] loadedChunks;

  public final @Nullable Long2IntOpenHashMap countByChunkTuple;
  public final @Nullable EnumMap<EntityType, MutableInt> totalCountByType;

  private int nextChunkIndex;

  public EntityScanSession(World world, @Nullable EntityType targetEntityType) {
    this.world = world;
    this.targetEntityType = targetEntityType;

    this.loadedChunks = world.getLoadedChunks();

    this.countByChunkTuple = targetEntityType != null ? new Long2IntOpenHashMap() : null;

    if (this.countByChunkTuple != null)
      this.countByChunkTuple.defaultReturnValue(0);

    this.totalCountByType = targetEntityType == null ? new EnumMap<>(EntityType.class) : null;
  }

  public void run(Plugin plugin, Runnable completionHandler) {
    var deadline = System.nanoTime() + MILLISECONDS_PER_TICK * 1_000_000L;

    while (nextChunkIndex < loadedChunks.length) {
      var chunk = loadedChunks[nextChunkIndex++];

      if (!world.isChunkLoaded(chunk.getX(), chunk.getZ()))
        continue;

      var chunkTuple = IntTuple.create(chunk.getX(), chunk.getZ());

      for (var entity : chunk.getEntities()) {
        if (countByChunkTuple != null) {
          if (entity.getType() == targetEntityType)
            countByChunkTuple.addTo(chunkTuple, 1);

          continue;
        }

        if (totalCountByType != null)
          totalCountByType.computeIfAbsent(entity.getType(), _ -> new MutableInt()).value++;
      }

      if (System.nanoTime() >= deadline)
        break;
    }

    if (nextChunkIndex >= loadedChunks.length) {
      completionHandler.run();
      return;
    }

    Bukkit.getScheduler().runTaskLater(plugin, () -> run(plugin, completionHandler), 1L);
  }
}

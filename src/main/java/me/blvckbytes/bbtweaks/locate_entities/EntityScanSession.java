package me.blvckbytes.bbtweaks.locate_entities;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

public class EntityScanSession {

  private static final int MILLISECONDS_PER_TICK = 5;

  public final World world;
  public final EntityType entityType;

  private final Chunk[] loadedChunks;

  public final Long2IntOpenHashMap countByChunkTuple;

  private int nextChunkIndex;

  public EntityScanSession(World world, EntityType entityType) {
    this.world = world;
    this.entityType = entityType;

    this.loadedChunks = world.getLoadedChunks();
    this.countByChunkTuple = new Long2IntOpenHashMap();
    this.countByChunkTuple.defaultReturnValue(0);
  }

  public void run(Plugin plugin, Runnable completionHandler) {
    var deadline = System.nanoTime() + MILLISECONDS_PER_TICK * 1_000_000L;

    while (nextChunkIndex < loadedChunks.length) {
      var chunk = loadedChunks[nextChunkIndex++];

      if (!world.isChunkLoaded(chunk.getX(), chunk.getZ()))
        continue;

      var chunkTuple = IntTuple.create(chunk.getX(), chunk.getZ());

      for (var entity : chunk.getEntities()) {
        if (entity.getType() != entityType)
          continue;

        countByChunkTuple.addTo(chunkTuple, 1);
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

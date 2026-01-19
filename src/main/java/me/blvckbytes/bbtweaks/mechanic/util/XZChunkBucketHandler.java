package me.blvckbytes.bbtweaks.mechanic.util;

import org.bukkit.World;

import java.util.List;

@FunctionalInterface
public interface XZChunkBucketHandler<T> {

  void handle(World world, int chunkX, int chunkY, List<T> entries);

}

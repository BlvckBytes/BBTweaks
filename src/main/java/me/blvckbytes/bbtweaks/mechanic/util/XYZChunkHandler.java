package me.blvckbytes.bbtweaks.mechanic.util;

@FunctionalInterface
public interface XYZChunkHandler {

  void handle(int chunkX, int chunkY, int chunkZ);

}

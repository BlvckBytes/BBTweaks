package me.blvckbytes.bbtweaks.mechanic.util;

@FunctionalInterface
public interface LineHandler {

  void handle(
    int minX, int minY, int minZ,
    int maxX, int maxY, int maxZ
  );

}

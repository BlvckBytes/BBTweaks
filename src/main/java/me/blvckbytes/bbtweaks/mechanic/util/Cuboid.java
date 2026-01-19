package me.blvckbytes.bbtweaks.mechanic.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

public record Cuboid(Block minPosition, Block maxPosition) {

  public void forEachXZChunk(XZChunkHandler handler) {
    for (var chunkX = minPosition.getX() >> 4; chunkX <= maxPosition.getX() >> 4; ++chunkX) {
      for (var chunkZ = minPosition.getZ() >> 4; chunkZ <= maxPosition.getZ() >> 4; ++chunkZ) {
        handler.handle(chunkX, chunkZ);
      }
    }
  }

  public void forEachXYZChunk(XYZChunkHandler handler) {
    for (var chunkX = minPosition.getX() >> 4; chunkX <= maxPosition.getX() >> 4; ++chunkX) {
      for (var chunkY = minPosition.getY() >> 4; chunkY <= maxPosition.getY() >> 4; ++chunkY) {
        for (var chunkZ = minPosition.getZ() >> 4; chunkZ <= maxPosition.getZ() >> 4; ++chunkZ) {
          handler.handle(chunkX, chunkY, chunkZ);
        }
      }
    }
  }

  public boolean doBoundsEqual(Cuboid other) {
    return doCoordinatesEqual(minPosition, other.minPosition) && doCoordinatesEqual(maxPosition, other.maxPosition);
  }

  private boolean doCoordinatesEqual(Block a, Block b) {
    return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
  }

  public void forEachLine(LineHandler lineHandler) {
    // Let there be a cuboid sitting at the origin of a cartesian coordinate-system with its
    // bottom left near corner, such that said corner will be the min-position, while the
    // maximum will lie diagonally opposite, in the top right far corner.

    // Bottom plane rectangle
    forEachLineInXZ(lineHandler, minPosition.getY());

    // Bottom left near corner <-> Top left near corner
    makeVerticalLine(lineHandler, minPosition.getX(), minPosition.getZ());

    // Bottom right near corner <-> Top right near corner
    makeVerticalLine(lineHandler, maxPosition.getX(), minPosition.getZ());

    // Bottom left far corner <-> Top left far corner
    makeVerticalLine(lineHandler, minPosition.getX(), maxPosition.getZ());

    // Bottom right far corner <-> Top right far corner
    makeVerticalLine(lineHandler, maxPosition.getX(), maxPosition.getZ());

    // Top plane rectangle
    forEachLineInXZ(lineHandler, maxPosition.getY());
  }

  private void makeVerticalLine(LineHandler lineHandler, int x, int z) {
    lineHandler.handle(
      x, minPosition.getY(), z,
      x, maxPosition.getY(), z
    );
  }

  private void forEachLineInXZ(LineHandler lineHandler, int y) {
    // Left near corner <-> Right near corner
    lineHandler.handle(
      minPosition.getX(), y, minPosition.getZ(),
      maxPosition.getX(), y, minPosition.getZ()
    );

    // Right near corner <-> Right far corner
    lineHandler.handle(
      maxPosition.getX(), y, minPosition.getZ(),
      maxPosition.getX(), y, maxPosition.getZ()
    );

    // Right far corner <-> Left far corner
    lineHandler.handle(
      minPosition.getX(), y, maxPosition.getZ(),
      maxPosition.getX(), y, maxPosition.getZ()
    );

    // Left far corner <-> Left near corner
    lineHandler.handle(
      minPosition.getX(), y, minPosition.getZ(),
      minPosition.getX(), y, maxPosition.getZ()
    );
  }

  // Shut up... Semantics >> bit-flip micro-optimization
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isPointInside(Location location) {
    var x = location.getX();
    var y = location.getY();
    var z = location.getZ();

    return (
      (x >= minPosition.getX() && x <= maxPosition.getX())
        && (y >= minPosition.getY() && y <= maxPosition.getY())
        && (z >= minPosition.getZ() && z <= maxPosition.getZ())
    );
  }
}

package me.blvckbytes.bbtweaks.mechanic.util;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class Cuboid {

  public final Block minPosition;
  public final Block maxPosition;

  private final Block maxExclusivePosition;

  public Cuboid(Block minPosition, Block maxPosition) {
    this.minPosition = minPosition;
    this.maxPosition = maxPosition;

    var doMinMaxCoincide = (
      maxPosition.getX() == minPosition.getX()
        && maxPosition.getY() == minPosition.getY()
        && maxPosition.getZ() == minPosition.getZ()
    );

    this.maxExclusivePosition = doMinMaxCoincide ? maxPosition : maxPosition.getRelative(-1, -1, -1);
  }

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

  public void forEachLine(boolean maxExclusive, LineHandler lineHandler) {
    // Let there be a cuboid sitting at the origin of a cartesian coordinate-system with its
    // bottom left near corner, such that said corner will be the min-position, while the
    // maximum will lie diagonally opposite, in the top right far corner.

    var chosenMaxPosition = maxExclusive ? maxExclusivePosition : maxPosition;

    // Bottom plane rectangle
    forEachLineInXZ(lineHandler, chosenMaxPosition, minPosition.getY());

    // Bottom left near corner <-> Top left near corner
    makeVerticalLine(lineHandler, chosenMaxPosition, minPosition.getX(), minPosition.getZ());

    // Bottom right near corner <-> Top right near corner
    makeVerticalLine(lineHandler, chosenMaxPosition, chosenMaxPosition.getX(), minPosition.getZ());

    // Bottom left far corner <-> Top left far corner
    makeVerticalLine(lineHandler, chosenMaxPosition, minPosition.getX(), chosenMaxPosition.getZ());

    // Bottom right far corner <-> Top right far corner
    makeVerticalLine(lineHandler, chosenMaxPosition, chosenMaxPosition.getX(), chosenMaxPosition.getZ());

    // Top plane rectangle
    forEachLineInXZ(lineHandler, chosenMaxPosition, chosenMaxPosition.getY());
  }

  private void makeVerticalLine(LineHandler lineHandler, Block chosenMaxPosition, int x, int z) {
    lineHandler.handle(
      x, minPosition.getY(), z,
      x, chosenMaxPosition.getY(), z,
      Axis.Y
    );
  }

  private void forEachLineInXZ(LineHandler lineHandler, Block chosenMaxPosition, int y) {
    // Left near corner <-> Right near corner
    lineHandler.handle(
      minPosition.getX(), y, minPosition.getZ(),
      chosenMaxPosition.getX(), y, minPosition.getZ(),
      Axis.X
    );

    // Right near corner <-> Right far corner
    lineHandler.handle(
      chosenMaxPosition.getX(), y, minPosition.getZ(),
      chosenMaxPosition.getX(), y, chosenMaxPosition.getZ(),
      Axis.Z
    );

    // Right far corner <-> Left far corner
    lineHandler.handle(
      minPosition.getX(), y, chosenMaxPosition.getZ(),
      chosenMaxPosition.getX(), y, chosenMaxPosition.getZ(),
      Axis.X
    );

    // Left far corner <-> Left near corner
    lineHandler.handle(
      minPosition.getX(), y, minPosition.getZ(),
      minPosition.getX(), y, chosenMaxPosition.getZ(),
      Axis.Z
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

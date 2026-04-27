package me.blvckbytes.bbtweaks.multi_break;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public record BlockDirections(BlockFace forwards, BlockFace left, BlockFace right) {

  public static BlockDirections determine(Vector lookingDirection) {
    var direction = directionToBlockFace(lookingDirection);

    return switch (direction) {
      case NORTH -> new BlockDirections(direction, BlockFace.WEST, BlockFace.EAST);
      case EAST -> new BlockDirections(direction, BlockFace.NORTH, BlockFace.SOUTH);
      case SOUTH -> new BlockDirections(direction, BlockFace.EAST, BlockFace.WEST);
      case WEST -> new BlockDirections(direction, BlockFace.SOUTH, BlockFace.NORTH);
      default -> throw new IllegalStateException("Expected only N/E/S/W to be passed into this switch");
    };
  }

  private static BlockFace directionToBlockFace(Vector direction) {
    var x = direction.getX();
    var z = direction.getZ();

    if (Math.abs(x) > Math.abs(z))
      return x > 0 ? BlockFace.EAST : BlockFace.WEST;

    return z > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
  }
}

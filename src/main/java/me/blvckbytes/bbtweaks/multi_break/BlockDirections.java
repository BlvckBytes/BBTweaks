package me.blvckbytes.bbtweaks.multi_break;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public record BlockDirections(BlockFace forwards, BlockFace left, BlockFace right, BlockFace up, BlockFace down) {

  public static BlockDirections determine(Player player) {
    var location = player.getLocation();
    var direction = directionToBlockFace(location.getDirection());

    if (Math.abs(location.getPitch()) >= 70) {
      var forwards = location.getPitch() < 0 ? BlockFace.UP : BlockFace.DOWN;

      return switch (direction) {
        case NORTH -> new BlockDirections(forwards, BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH);
        case EAST -> new BlockDirections(forwards, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
        case SOUTH -> new BlockDirections(forwards, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);
        case WEST -> new BlockDirections(forwards, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.WEST, BlockFace.EAST);
        default -> throw new IllegalStateException("Expected only N/E/S/W to be passed into this switch");
      };
    }

    return switch (direction) {
      case NORTH -> new BlockDirections(direction, BlockFace.WEST, BlockFace.EAST, BlockFace.UP, BlockFace.DOWN);
      case EAST -> new BlockDirections(direction, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN);
      case SOUTH -> new BlockDirections(direction, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
      case WEST -> new BlockDirections(direction, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.UP, BlockFace.DOWN);
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

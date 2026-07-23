package me.blvckbytes.bbtweaks.mechanic;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;

public record MechanicSignInfo(
  BlockFace signFacing,
  Block mountBlock,
  Block inputBlock
) {
  public static MechanicSignInfo createFromSign(Sign sign) {
    var blockData = sign.getBlockData();

    BlockFace cardinalFacing;
    BlockFace mountFace;

    if (blockData instanceof Rotatable rotatable) {
      cardinalFacing = toCardinalFace(rotatable.getRotation());
      mountFace = BlockFace.DOWN;
    }

    else if (blockData instanceof Directional directional) {
      cardinalFacing = directional.getFacing();
      mountFace = cardinalFacing.getOppositeFace();
    }

    else
      throw new IllegalArgumentException("Encountered neither a wall- nor a standing-sign: " + sign);

    var signBlock = sign.getBlock();

    return new MechanicSignInfo(
      cardinalFacing,
      signBlock.getRelative(mountFace),
      signBlock.getRelative(cardinalFacing)
    );
  }

  private static BlockFace toCardinalFace(BlockFace blockFace) {
    return switch (blockFace) {
      case NORTH, NORTH_NORTH_WEST, NORTH_NORTH_EAST, NORTH_EAST -> BlockFace.NORTH;
      case EAST,  EAST_SOUTH_EAST,  EAST_NORTH_EAST,  NORTH_WEST -> BlockFace.EAST;
      case SOUTH, SOUTH_SOUTH_WEST, SOUTH_SOUTH_EAST, SOUTH_EAST -> BlockFace.SOUTH;
      case WEST,  WEST_NORTH_WEST,  WEST_SOUTH_WEST,  SOUTH_WEST -> BlockFace.WEST;
      default -> throw new IllegalArgumentException("Cannot map " + blockFace + " to a cardinal face");
    };
  }
}

package me.blvckbytes.bbtweaks.teleporter_sign;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public record TeleporterSignParameters(
  TeleporterSignCoordinates coordinates,
  EnumSet<TeleporterSignFlag> flags
) {

  public void teleport(Player player) {
    player.teleport(new Location(
      player.getWorld(),
      coordinates.x(),
      coordinates.y() + 1,
      coordinates.z(),
      facingToYaw(getFacing()), 0
    ));
  }

  public BlockFace getFacing() {
    if (flags.contains(TeleporterSignFlag.EAST))
      return BlockFace.EAST;

    if (flags.contains(TeleporterSignFlag.SOUTH))
      return BlockFace.SOUTH;

    if (flags.contains(TeleporterSignFlag.WEST))
      return BlockFace.WEST;

    return BlockFace.NORTH;
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("x", coordinates.x())
      .withVariable("y", coordinates.y())
      .withVariable("z", coordinates.z())
      .withVariable("facing", getFacing().name());
  }

  private float facingToYaw(BlockFace facing) {
    return switch (facing) {
      case NORTH -> 180F;
      case EAST -> -90F;
      case SOUTH -> 0F;
      case WEST -> 90F;
      default -> throw new IllegalArgumentException();
    };
  }
}

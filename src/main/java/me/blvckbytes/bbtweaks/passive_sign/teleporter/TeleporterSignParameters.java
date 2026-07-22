package me.blvckbytes.bbtweaks.passive_sign.teleporter;

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

    if (flags.contains(TeleporterSignFlag.NORTH_EAST))
      return BlockFace.NORTH_EAST;

    if (flags.contains(TeleporterSignFlag.NORTH_WEST))
      return BlockFace.NORTH_WEST;

    if (flags.contains(TeleporterSignFlag.SOUTH_EAST))
      return BlockFace.SOUTH_EAST;

    if (flags.contains(TeleporterSignFlag.SOUTH_WEST))
      return BlockFace.SOUTH_WEST;

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
      case NORTH -> 180;
      case NORTH_EAST -> -135;
      case NORTH_WEST -> 135;
      case EAST -> -90;
      case SOUTH -> 0;
      case SOUTH_EAST -> -45;
      case SOUTH_WEST -> 45;
      case WEST -> 90;
      default -> throw new IllegalArgumentException();
    };
  }
}

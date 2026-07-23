package me.blvckbytes.bbtweaks.mechanic.teleporter;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class TeleporterInstance extends SISOInstance {

  public final EnumSet<TeleporterFlag> flags;
  public final TeleporterCoordinates coordinates;

  public TeleporterInstance(
    Sign sign,
    Side side,
    EnumSet<TeleporterFlag> flags,
    TeleporterCoordinates coordinates
  ) {
    super(sign, side);

    this.flags = flags;
    this.coordinates = coordinates;
  }

  public void teleport(Player player) {
    player.teleport(new Location(
      player.getWorld(),
      coordinates.x(),
      coordinates.y() + 1,
      coordinates.z(),
      facingToYaw(getFacing()), 0
    ));
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("target_x", coordinates.x())
      .withVariable("target_y", coordinates.y())
      .withVariable("target_z", coordinates.z())
      .withVariable("target_facing", getFacing().name());
  }

  private BlockFace getFacing() {
    if (flags.contains(TeleporterFlag.EAST))
      return BlockFace.EAST;

    if (flags.contains(TeleporterFlag.SOUTH))
      return BlockFace.SOUTH;

    if (flags.contains(TeleporterFlag.WEST))
      return BlockFace.WEST;

    if (flags.contains(TeleporterFlag.NORTH_EAST))
      return BlockFace.NORTH_EAST;

    if (flags.contains(TeleporterFlag.NORTH_WEST))
      return BlockFace.NORTH_WEST;

    if (flags.contains(TeleporterFlag.SOUTH_EAST))
      return BlockFace.SOUTH_EAST;

    if (flags.contains(TeleporterFlag.SOUTH_WEST))
      return BlockFace.SOUTH_WEST;

    return BlockFace.NORTH;
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

  @Override
  public boolean tick(long time) {
    return true;
  }
}

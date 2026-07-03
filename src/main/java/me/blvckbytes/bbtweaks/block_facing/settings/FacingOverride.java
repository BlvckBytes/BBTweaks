package me.blvckbytes.bbtweaks.block_facing.settings;

import me.blvckbytes.bbtweaks.multi_break.BlockDirections;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum FacingOverride implements MatchableEnum {
  // NOTE: The ordinal of this enum is used as the main identifier!
  NORTH,
  EAST,
  SOUTH,
  WEST,
  UP,
  DOWN,
  IN_LOOKING_DIRECTION,
  AGAINST_LOOKING_DIRECTION,
  ;

  public static List<FacingOverride> ALL_VALUES = List.of(values());

  public static final FacingOverride DEFAULT_VALUE = IN_LOOKING_DIRECTION;

  public static final EnumMatcher<FacingOverride> matcher = new EnumMatcher<>(values());

  public BlockFace getFace(Player player) {
    return switch (this) {
      case NORTH -> BlockFace.NORTH;
      case EAST -> BlockFace.EAST;
      case SOUTH -> BlockFace.SOUTH;
      case WEST -> BlockFace.WEST;
      case UP -> BlockFace.UP;
      case DOWN -> BlockFace.DOWN;
      case IN_LOOKING_DIRECTION -> determineLookingDirection(player);
      case AGAINST_LOOKING_DIRECTION -> determineLookingDirection(player).getOppositeFace();
    };
  }

  public static @Nullable FacingOverride byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }

  private static BlockFace determineLookingDirection(Player player) {
    var location = player.getLocation();

    if (location.getPitch() >= 70)
      return BlockFace.DOWN;

    if (location.getPitch() <= -70)
      return BlockFace.UP;

    return BlockDirections.directionToBlockFace(location.getDirection());
  }
}

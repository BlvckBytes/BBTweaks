package me.blvckbytes.bbtweaks.block_facing.settings;

import me.blvckbytes.bbtweaks.multi_break.BlockDirections;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum FacingOverride implements MatchableEnum {
  // NOTE: The ordinal of this enum is used as the main identifier!
  NORTH("N"),
  EAST("E"),
  SOUTH("S"),
  WEST("W"),
  UP("U"),
  DOWN("D"),
  IN_LOOKING_DIRECTION("L"),
  AGAINST_LOOKING_DIRECTION("-L"),
  RANDOM_UP_DOWN("R/UD"),
  RANDOM_CARDINAL("R/C"),
  RANDOM_ALL("R/A"),
  ;

  public static List<FacingOverride> ALL_VALUES = List.of(values());

  public static final FacingOverride DEFAULT_VALUE = IN_LOOKING_DIRECTION;

  public static final EnumMatcher<FacingOverride> matcher = new EnumMatcher<>(values());

  private static final BlockFace[] UP_DOWN_FACES = {
    BlockFace.UP, BlockFace.DOWN
  };

  private static final BlockFace[] CARDINAL_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private static final BlockFace[] ALL_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
    BlockFace.UP, BlockFace.DOWN
  };

  public final String sidebarShorthand;

  FacingOverride(String sidebarShorthand) {
    this.sidebarShorthand = sidebarShorthand;
  }

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
      case RANDOM_UP_DOWN -> UP_DOWN_FACES[ThreadLocalRandom.current().nextInt(UP_DOWN_FACES.length)];
      case RANDOM_CARDINAL -> CARDINAL_FACES[ThreadLocalRandom.current().nextInt(CARDINAL_FACES.length)];
      case RANDOM_ALL -> ALL_FACES[ThreadLocalRandom.current().nextInt(ALL_FACES.length)];
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

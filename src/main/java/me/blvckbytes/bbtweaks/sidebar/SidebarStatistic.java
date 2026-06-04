package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.component_markup.constructor.SlotType;
import me.blvckbytes.bbtweaks.sidebar.config.StatisticSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum SidebarStatistic {
  // NOTE: The ordinal of this enum is used as the main identifier!
  GROUP_PREFIX,
  MONEY,
  TOTAL_PLAYTIME,
  TOTAL_PLAYTIME_TOP_PLACE,
  TOTAL_AFKTIME,
  TOTAL_AFKTIME_TOP_PLACE,
  HOME_COUNT,
  PING,
  DATE,
  REAL_TIME,
  COORDINATES,
  BIOME,
  LOOKING_DIRECTION,
  GAME_TIME,
  FIRST_JOB_PROGRESSION,
  SECOND_JOB_PROGRESSION,
  MCMMO_POWER_LEVEL,
  PLAYER_NAME,
  TPS,
  LIGHT_LEVEL,
  MULTIBREAK_STATUS,
  INV_MAGNET_STATUS,
  INV_FILTER_STATUS,
  AUTOTOOL_STATUS,
  CURRENT_AFK_DURATION,
  REMAINING_PLAYTIME_UNTIL_NEXT_RANK,
  ;

  public static final List<SidebarStatistic> ALL_VALUES = List.of(values());

  public @Nullable Component renderFor(
    BoardHolder holder,
    StatisticSection statistic,
    SidebarPreferences preferences,
    StatisticEnvironmentResolver environmentResolver
  ) {
    var result = statistic.render.interpret(
      SlotType.SINGLE_LINE_CHAT,
      environmentResolver.resolve(holder, this)
        .withVariable("label_color", preferences.labelColorByStatistic.get(statistic._sidebarStatistic).hexColor())
        .withVariable("value_color", preferences.valueColor.hexColor())
    ).getFirst();

    // Empty results are not rendered at all, which enables renderers to conditionally display themselves.
    if (result.equals(Component.empty()))
      return null;

    return result;
  }

  public static @Nullable SidebarStatistic byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }
}

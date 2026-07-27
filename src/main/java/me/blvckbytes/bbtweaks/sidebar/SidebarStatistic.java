package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.component_markup.constructor.SlotType;
import me.blvckbytes.bbtweaks.sidebar.config.StatisticSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.sidebar.preferences.StatisticEnableMode;
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
  REMAINING_SHOP_REGION_RENT_DURATION,
  REMAINING_CREATIVE_REGION_RENT_DURATION,
  SPACER_NUMBER_ONE(true),
  SPACER_NUMBER_TWO(true),
  SPACER_NUMBER_THREE(true),
  AUTO_PICKUP_CONTAINER_USAGE_ABSOLUTE,
  AUTO_PICKUP_CONTAINER_USAGE_RELATIVE,
  BLOCK_FACING_STATUS,
  HOTBAR_RANDOMIZER_STATUS,
  PLAYER_COUNT,
  DEATH_COUNT,
  WORLD_NAME,
  MCMMO_ACROBATICS_LEVEL,
  MCMMO_ALCHEMY_LEVEL,
  MCMMO_ARCHERY_LEVEL,
  MCMMO_AXES_LEVEL,
  MCMMO_CROSSBOWS_LEVEL,
  MCMMO_EXCAVATION_LEVEL,
  MCMMO_FISHING_LEVEL,
  MCMMO_HERBALISM_LEVEL,
  MCMMO_MACES_LEVEL,
  MCMMO_MINING_LEVEL,
  MCMMO_REPAIR_LEVEL,
  MCMMO_SALVAGE_LEVEL,
  MCMMO_SMELTING_LEVEL,
  MCMMO_SPEARS_LEVEL,
  MCMMO_SWORDS_LEVEL,
  MCMMO_TAMING_LEVEL,
  MCMMO_TRIDENTS_LEVEL,
  MCMMO_UNARMED_LEVEL,
  MCMMO_WOODCUTTING_LEVEL,
  ;

  public final boolean isSpacer;

  public static final List<SidebarStatistic> ALL_VALUES = List.of(values());

  SidebarStatistic() {
    this(false);
  }

  SidebarStatistic(boolean isSpacer) {
    this.isSpacer = isSpacer;
  }

  public @Nullable Component renderFor(
    BoardHolder holder,
    StatisticSection statisticSection,
    SidebarPreferences preferences,
    StatisticEnvironmentResolver environmentResolver
  ) {
    var enableMode = preferences.enableModeByStatistic.get(statisticSection._sidebarStatistic);

    if (enableMode == StatisticEnableMode.OFF)
      return null;

    var result = statisticSection.render.interpret(
      SlotType.SINGLE_LINE_CHAT,
      environmentResolver.resolve(holder, this)
        .withVariable("label_style", preferences.labelStyleByStatistic.get(statisticSection._sidebarStatistic))
        .withVariable("value_style", preferences.valueStyleByStatistic.get(statisticSection._sidebarStatistic))
        .withVariable("show_icon", preferences.showIcons)
        .withVariable("show_label", enableMode.showLabel)
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

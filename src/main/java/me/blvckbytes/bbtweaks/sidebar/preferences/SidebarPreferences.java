package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class SidebarPreferences {

  // TODO: Have different defaults per preferences-slot.

  private static final boolean DEFAULT_SHOW_TITLE = true;
  private static final boolean DEFAULT_SHOW_ICONS = true;
  private static final boolean DEFAULT_DO_SCROLL = true;

  public final SidebarPreferencesSlots preferencesSlots;
  public final int slotIndex;
  private final ConfigKeeper<MainSection> config;

  public boolean showTitle;
  public boolean showIcons;
  public boolean doScroll;
  public DelimitersMode delimitersMode;
  public SneakMode sneakMode;

  public final EnumMap<SidebarStatistic, StatisticEnableMode> enableModeByStatistic;
  public final EnumMap<SidebarStatistic, ColorAndFormats> labelStyleByStatistic;
  public final EnumMap<SidebarStatistic, ColorAndFormats> valueStyleByStatistic;
  public final List<SidebarStatistic> statisticsInOrder;

  public SidebarPreferences(
    SidebarPreferencesSlots preferencesSlots,
    int slotIndex,
    ConfigKeeper<MainSection> config
  ) {
    this.preferencesSlots = preferencesSlots;
    this.slotIndex = slotIndex;
    this.config = config;

    this.enableModeByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.labelStyleByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.valueStyleByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.statisticsInOrder = new ArrayList<>();

    this.resetToDefaults();
  }

  public boolean divergesFromDefaults() {
    if (showTitle != DEFAULT_SHOW_TITLE)
      return true;

    if (showIcons != DEFAULT_SHOW_ICONS)
      return true;

    if (doScroll != DEFAULT_DO_SCROLL)
      return true;

    if (sneakMode != SneakMode.DEFAULT_VALUE)
      return true;

    if (delimitersMode != DelimitersMode.DEFAULT_VALUE)
      return true;

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (statisticsInOrder.get(statistic.ordinal()) != statistic)
        return true;

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      if (!labelStyleByStatistic.get(statistic).equals(statisticSection._defaultLabelStyle))
        return true;

      if (!valueStyleByStatistic.get(statistic).equals(statisticSection._defaultValueStyle))
        return true;

      if (enableModeByStatistic.get(statistic) != statisticSection._defaultEnableMode)
        return true;
    }

    return false;
  }

  public void resetToDefaults() {
    this.showTitle = DEFAULT_SHOW_TITLE;
    this.showIcons = DEFAULT_SHOW_ICONS;
    this.doScroll = DEFAULT_DO_SCROLL;
    this.sneakMode = SneakMode.DEFAULT_VALUE;
    this.delimitersMode = DelimitersMode.DEFAULT_VALUE;

    this.statisticsInOrder.clear();

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      enableModeByStatistic.put(statistic, statisticSection._defaultEnableMode);

      // We're modifying styles in place, so cloning before setting is crucial as
      // to not mess up the stored defaults by editing them by reference.
      labelStyleByStatistic.put(statistic, new ColorAndFormats(statisticSection._defaultLabelStyle));
      valueStyleByStatistic.put(statistic, new ColorAndFormats(statisticSection._defaultValueStyle));

      statisticsInOrder.add(statistic);
    }
  }

  public void onConfigReload() {
    for (var value : labelStyleByStatistic.values())
      value.color = config.rootSection.sidebar.tryGetCurrentColorWithEqualName(value.color);

    for (var value : valueStyleByStatistic.values())
      value.color = config.rootSection.sidebar.tryGetCurrentColorWithEqualName(value.color);
  }

  public Component getBoardTitle() {
    if (!showTitle)
      return Component.text(" ");

    return config.rootSection.sidebar.boardTitle.interpret(
      SlotType.SINGLE_LINE_CHAT,
      new InterpretationEnvironment()
        .withVariable("name", preferencesSlots.player.getName())
        .withVariable("display_name", preferencesSlots.player.displayName())
    ).getFirst();
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("enabled", preferencesSlots.enabled)
      .withVariable("slot_index", slotIndex)
      .withVariable("show_title", showTitle)
      .withVariable("show_icons", showIcons)
      .withVariable("do_scroll", doScroll)
      .withVariable("delimiters_mode", delimitersMode.name())
      .withVariable("slot_enabled", preferencesSlots.getSelectedSlotIndex() == slotIndex)
      .withVariable("sneak_mode", sneakMode.name());
  }
}

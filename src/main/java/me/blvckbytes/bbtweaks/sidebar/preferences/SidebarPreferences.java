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
    var defaults = config.rootSection.sidebar.getDefaultsForSlot(slotIndex);

    if (showTitle != defaults.showTitle)
      return true;

    if (showIcons != defaults.showIcons)
      return true;

    if (doScroll != defaults.doScroll)
      return true;

    if (sneakMode != defaults.sneakMode)
      return true;

    if (delimitersMode != defaults.delimitersMode)
      return true;

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (statisticsInOrder.get(statistic.ordinal()) != statistic)
        return true;

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      if (!labelStyleByStatistic.get(statistic).equals(statisticSection._defaultLabelStyle))
        return true;

      if (!valueStyleByStatistic.get(statistic).equals(statisticSection._defaultValueStyle))
        return true;

      if (enableModeByStatistic.get(statistic) != defaults._enableModeByStatistic.get(statistic))
        return true;
    }

    return false;
  }

  public void resetToDefaults() {
    var defaults = config.rootSection.sidebar.getDefaultsForSlot(slotIndex);

    this.showTitle = defaults.showTitle;
    this.showIcons = defaults.showIcons;
    this.doScroll = defaults.doScroll;
    this.sneakMode = defaults.sneakMode;
    this.delimitersMode = defaults.delimitersMode;

    this.statisticsInOrder.clear();

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      enableModeByStatistic.put(statistic, defaults._enableModeByStatistic.get(statistic));

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

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

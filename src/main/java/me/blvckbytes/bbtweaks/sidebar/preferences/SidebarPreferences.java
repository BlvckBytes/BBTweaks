package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public class SidebarPreferences {

  // TODO: idea - toggleable icons

  private static final boolean DEFAULT_ENABLED = false;
  private static final boolean DEFAULT_SHOW_TITLE = true;

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  public boolean showTitle;
  public DelimitersMode delimitersMode;
  public SneakMode sneakMode;

  public final EnumSet<SidebarStatistic> enabledStatistics;
  public final EnumMap<SidebarStatistic, ColorAndFormats> labelStyleByStatistic;
  public final EnumMap<SidebarStatistic, ColorAndFormats> valueStyleByStatistic;
  public final List<SidebarStatistic> statisticsInOrder;

  public SidebarPreferences(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    // This flag is consciously excluded from the public reset-API.
    this.enabled = DEFAULT_ENABLED;

    this.enabledStatistics = EnumSet.noneOf(SidebarStatistic.class);
    this.labelStyleByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.valueStyleByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.statisticsInOrder = new ArrayList<>();

    this.resetToDefaults();
  }

  public boolean divergesFromDefaults() {
    if (showTitle != DEFAULT_SHOW_TITLE)
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

      if (enabledStatistics.contains(statistic) != statisticSection.defaultEnabled)
        return true;
    }

    return false;
  }

  public void resetToDefaults() {
    this.showTitle = DEFAULT_SHOW_TITLE;
    this.sneakMode = SneakMode.DEFAULT_VALUE;
    this.delimitersMode = DelimitersMode.DEFAULT_VALUE;

    this.enabledStatistics.clear();
    this.statisticsInOrder.clear();

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      if (statisticSection.defaultEnabled)
        enabledStatistics.add(statistic);

      // We're modifying styles in place, so cloning before setting is crucial as
      // to not mess up the stored defaults by editing them by reference.
      labelStyleByStatistic.put(statistic, new ColorAndFormats(statisticSection._defaultLabelStyle));
      valueStyleByStatistic.put(statistic, new ColorAndFormats(statisticSection._defaultValueStyle));

      statisticsInOrder.add(statistic);
    }
  }

  public void toggleEnabled() {
    enabled ^= true;

    if (enabled)
      config.rootSection.sidebar.sidebarNowEnabled.sendMessage(player);
    else
      config.rootSection.sidebar.sidebarNowDisabled.sendMessage(player);
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
        .withVariable("name", player.getName())
        .withVariable("display_name", player.displayName())
    ).getFirst();
  }
}

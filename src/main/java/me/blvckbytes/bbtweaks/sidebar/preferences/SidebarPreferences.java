package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.config.NamedColor;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public class SidebarPreferences {

  private static final boolean DEFAULT_ENABLED = false;
  private static final boolean DEFAULT_SHOW_TITLE = true;
  private static final SneakMode DEFAULT_SNEAK_MODE = SneakMode.NONE;

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public NamedColor valueColor;

  public boolean enabled;
  public boolean showTitle;
  public SneakMode sneakMode;

  public final EnumSet<SidebarStatistic> enabledStatistics;
  public final EnumMap<SidebarStatistic, NamedColor> labelColorByStatistic;
  public final List<SidebarStatistic> statisticsInOrder;

  public SidebarPreferences(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    // This flag is consciously excluded from the public reset-API.
    this.enabled = DEFAULT_ENABLED;

    this.enabledStatistics = EnumSet.noneOf(SidebarStatistic.class);
    this.labelColorByStatistic = new EnumMap<>(SidebarStatistic.class);
    this.statisticsInOrder = new ArrayList<>();

    this.resetToDefaults();
  }

  public boolean divergesFromDefaults() {
    if (showTitle != DEFAULT_SHOW_TITLE)
      return true;

    if (sneakMode != DEFAULT_SNEAK_MODE)
      return true;

    if (this.valueColor != config.rootSection.sidebar._defaultValueColor)
      return true;

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (statisticsInOrder.get(statistic.ordinal()) != statistic)
        return true;

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      if (labelColorByStatistic.get(statistic) != statisticSection._defaultLabelColor)
        return true;

      if (enabledStatistics.contains(statistic) != statisticSection.defaultEnabled)
        return true;
    }

    return false;
  }

  public void resetToDefaults() {
    this.showTitle = DEFAULT_SHOW_TITLE;
    this.sneakMode = DEFAULT_SNEAK_MODE;

    this.valueColor = config.rootSection.sidebar._defaultValueColor;

    this.enabledStatistics.clear();
    this.statisticsInOrder.clear();

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      if (statisticSection.defaultEnabled)
        enabledStatistics.add(statistic);

      labelColorByStatistic.put(statistic, statisticSection._defaultLabelColor);
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
    this.valueColor = config.rootSection.sidebar.tryGetCurrentColorWithEqualName(valueColor);

    for (var entry : labelColorByStatistic.entrySet())
      entry.setValue(config.rootSection.sidebar.tryGetCurrentColorWithEqualName(entry.getValue()));
  }

  public Component getBoardTitle() {
    if (!showTitle)
      return Component.empty();

    return config.rootSection.sidebar.boardTitle.interpret(
      SlotType.SINGLE_LINE_CHAT,
      new InterpretationEnvironment()
        .withVariable("name", player.getName())
        .withVariable("display_name", player.displayName())
    ).getFirst();
  }
}

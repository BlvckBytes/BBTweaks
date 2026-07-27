package me.blvckbytes.bbtweaks.sidebar.preferences;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;

import java.lang.reflect.Field;
import java.util.*;

public class SidebarDefaultsSection extends ConfigSection {

  public boolean showTitle;
  public boolean showIcons;
  public boolean doScroll;

  public SneakMode sneakMode;
  public DelimitersMode delimitersMode;
  public AutoSortMode autoSortMode;

  public final List<String> enabledStatistics = new ArrayList<>();

  public @CSIgnore EnumMap<SidebarStatistic, StatisticEnableMode> _enableModeByStatistic = new EnumMap<>(SidebarStatistic.class);

  public SidebarDefaultsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (sneakMode == null)
      throw new MappingError("Property \"sneakMode\" must not be absent");

    if (delimitersMode == null)
      throw new MappingError("Property \"delimitersMode\" must not be absent");

    if (autoSortMode == null)
      throw new MappingError("Property \"autoSortMode\" must not be absent");

    for (var statisticName : enabledStatistics) {
      statisticName = statisticName.trim().toUpperCase();

      SidebarStatistic statistic;

      try {
        statistic = SidebarStatistic.valueOf(statisticName);
      } catch (Throwable _) {
        throw new MappingError("Could not find a statistic named \"" + statisticName + "\" for property \"enableModeByStatistic\"");
      }

      _enableModeByStatistic.put(statistic, StatisticEnableMode.ON);
    }

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (_enableModeByStatistic.containsKey(statistic))
        continue;

      _enableModeByStatistic.put(statistic, StatisticEnableMode.OFF);
    }
  }
}

package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.color.PackedColor;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.color_display.ColorDisplaySection;
import me.blvckbytes.bbtweaks.sidebar.command.SidebarCommandSection;
import me.blvckbytes.bbtweaks.sidebar.settings_display.SettingsDisplaySection;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SidebarSortingDisplaySection;

import java.lang.reflect.Field;
import java.util.*;

@CSAlways
public class SidebarSection extends ConfigSection {

  public SidebarCommandSection command;

  public ComponentMarkup allDeactivatedItemsAlreadyAtEnd;
  public ComponentMarkup movedAllDeactivatedItemsToEnd;
  public ComponentMarkup entryAlreadyAtTheVeryBeginning;
  public ComponentMarkup entryAlreadyAtTheVeryEnd;
  public ComponentMarkup sidebarNowEnabled;
  public ComponentMarkup sidebarNowDisabled;

  public int doubleSneakMaxDelayMs;
  public int scrollIntervalTicks;
  public int updateIntervalTicks;

  public ComponentMarkup boardTitle;

  public ComponentMarkup defaultValueColor;
  public @CSIgnore NamedColor _defaultValueColor;

  public Map<String, StatisticSection> statistics = new LinkedHashMap<>();

  public Map<String, ColorSection> colors = new LinkedHashMap<>();

  public @CSIgnore List<NamedColor> _colors = new ArrayList<>();
  public @CSIgnore Map<String, NamedColor> _colorByNameLower = new HashMap<>();

  public ColorDisplaySection colorDisplay;
  public SettingsDisplaySection settingsDisplay;
  public SidebarSortingDisplaySection sortingDisplay;

  @CSIgnore
  public final EnumMap<SidebarStatistic, StatisticSection> _statisticsMap = new EnumMap<>(SidebarStatistic.class);

  @CSIgnore
  public final List<StatisticSection> _statistics = new ArrayList<>();

  public SidebarSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var colorEntry : colors.entrySet()) {
      var name = colorEntry.getKey().trim();
      var colorSection = colorEntry.getValue();

      var value = colorSection.color.asPlainString(null);
      var packedColor = PackedColor.tryParse(value);

      if (packedColor == PackedColor.NULL_SENTINEL)
        throw new MappingError("Encountered malformed color-value \"" + value + "\" for name \"" + name + "\"");

      var hexColor = PackedColor.asNonAlphaHex(packedColor);

      var color = new NamedColor(
        name,
        colorSection._iconType,
        colorSection.displayName.interpret(
          SlotType.SINGLE_LINE_CHAT,
          new InterpretationEnvironment()
            .withVariable("color", hexColor)
        ).getFirst(),
        hexColor
      );

      _colors.add(color);
      _colorByNameLower.put(name.toLowerCase(), color);
    }

    for (var entry : statistics.entrySet()) {
      var statisticName = entry.getKey().toUpperCase().trim();

      SidebarStatistic statistic;

      try {
        statistic = SidebarStatistic.valueOf(statisticName);
      } catch (Throwable e) {
        throw new MappingError("Unknown statistic: " + statisticName);
      }

      var statisticSection = entry.getValue();

      var defaultLabelColorName = statisticSection.defaultLabelColor.asPlainString(null);
      statisticSection._defaultLabelColor = _colorByNameLower.get(defaultLabelColorName.toLowerCase());

      if (statisticSection._defaultLabelColor == null)
        throw new MappingError("Could not find a named color of \"" + defaultLabelColorName + "\" for the defaultLabelColor of statistic " + statisticName);

      statisticSection._sidebarStatistic = statistic;

      _statisticsMap.put(statistic, statisticSection);
      _statistics.add(statisticSection);
    }

    for (var statistic : SidebarStatistic.ALL_VALUES) {
      if (!_statisticsMap.containsKey(statistic))
        throw new MappingError("Missing statistic: " + statistic.name());
    }

    var defaultValueColorName = defaultValueColor.asPlainString(null);
    _defaultValueColor = _colorByNameLower.get(defaultValueColorName.toLowerCase());

    if (_defaultValueColor == null)
      throw new MappingError("Could not find the named color of \"" + defaultValueColorName + "\" specified on the property \"defaultValueColor\"");

    if (doubleSneakMaxDelayMs <= 0)
      throw new MappingError("Property \"doubleSneakMaxDelayMs\" cannot be less than or equal to zero");

    if (scrollIntervalTicks <= 0)
      throw new MappingError("Property \"scrollIntervalTicks\" cannot be less than or equal to zero");

    if (updateIntervalTicks <= 0)
      throw new MappingError("Property \"updateIntervalTicks\" cannot be less than or equal to zero");
  }

  public NamedColor tryGetCurrentColorWithEqualName(NamedColor color) {
    var result = _colorByNameLower.get(color.name().toLowerCase());

    if (result != null)
      return result;

    return color;
  }
}

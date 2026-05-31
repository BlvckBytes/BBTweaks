package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;

public class StatisticSection extends ConfigSection {

  public ComponentMarkup render;
  public ComponentMarkup defaultLabelColor;
  public boolean defaultEnabled;

  public @CSAlways StatisticIconData iconData;
  public @CSIgnore NamedColor _defaultLabelColor;
  public @CSIgnore SidebarStatistic _sidebarStatistic;

  public StatisticSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

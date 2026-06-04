package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.sidebar.preferences.ColorAndFormats;

public class StatisticSection extends ConfigSection {

  public ComponentMarkup render;

  public @CSAlways ColorAndFormatsSection defaultLabelStyle;
  public ColorAndFormats _defaultLabelStyle;

  public @CSAlways ColorAndFormatsSection defaultValueStyle;
  public ColorAndFormats _defaultValueStyle;

  public boolean defaultEnabled;

  public @CSAlways StatisticIconData iconData;
  public @CSIgnore SidebarStatistic _sidebarStatistic;

  public StatisticSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

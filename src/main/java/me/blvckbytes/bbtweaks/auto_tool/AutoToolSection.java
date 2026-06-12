package me.blvckbytes.bbtweaks.auto_tool;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AutoToolSection extends ConfigSection {

  public @CSAlways AutoToolCommandSection command;

  public ComponentMarkup noPermission;
  public ComponentMarkup nowEnabled;
  public ComponentMarkup nowDisabled;

  public AutoToolSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

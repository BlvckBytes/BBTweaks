package me.blvckbytes.bbtweaks.mechanic.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class CommandSignSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup missingCommand;
  public ComponentMarkup creationSuccess;

  public CommandSignSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

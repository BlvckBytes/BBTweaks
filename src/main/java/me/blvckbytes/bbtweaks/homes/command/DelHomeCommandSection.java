package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DelHomeCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "delhome";

  public ComponentMarkup commandUsage;
  public ComponentMarkup homeDoesNotExist;
  public ComponentMarkup homeDeleted;

  public DelHomeCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

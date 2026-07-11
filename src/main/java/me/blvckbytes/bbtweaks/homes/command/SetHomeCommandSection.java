package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SetHomeCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "sethome";

  public ComponentMarkup commandUsage;
  public ComponentMarkup homeSet;
  public ComponentMarkup homeOverwritten;

  public SetHomeCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

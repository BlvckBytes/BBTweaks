package me.blvckbytes.bbtweaks.pipes.search.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PipeFetchCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "pipefetch";

  public ComponentMarkup usage;
  public ComponentMarkup malformedMaximumAmount;
  public ComponentMarkup nonPositiveMaximumAmount;

  public PipeFetchCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

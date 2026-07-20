package me.blvckbytes.bbtweaks.pipes.predicates.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PipePredicatesCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "pipepredicates";

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;
  public ComponentMarkup actionUsage;

  public PipePredicatesCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

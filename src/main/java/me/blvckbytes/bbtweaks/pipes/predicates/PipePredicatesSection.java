package me.blvckbytes.bbtweaks.pipes.predicates;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.pipes.predicates.command.PipePredicatesCommandSection;

public class PipePredicatesSection extends ConfigSection {

  @CSAlways
  public PipePredicatesCommandSection command;

  public ComponentMarkup manualEditWhileInPredicateMode;

  public PipePredicatesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

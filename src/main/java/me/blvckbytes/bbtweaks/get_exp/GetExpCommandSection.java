package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class GetExpCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "getexp";

  public GetExpCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

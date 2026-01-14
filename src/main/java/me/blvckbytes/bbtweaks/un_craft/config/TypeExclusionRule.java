package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class TypeExclusionRule extends IOTypeRule {

  public String reason;

  public TypeExclusionRule(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

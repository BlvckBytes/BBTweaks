package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.ArrayList;
import java.util.List;

public class ResultSubtractionRule extends TypeRule {

  public List<TypeRule> subtractedMaterials = new ArrayList<>();

  public ResultSubtractionRule(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

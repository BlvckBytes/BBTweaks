package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.ArrayList;
import java.util.List;

public class ResultSubtractionRuleSection extends TypeRuleSection {

  public List<TypeRuleSection> subtractedMaterials = new ArrayList<>();

  public ResultSubtractionRuleSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

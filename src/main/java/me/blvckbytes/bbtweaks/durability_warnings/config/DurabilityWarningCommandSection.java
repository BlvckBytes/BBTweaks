package me.blvckbytes.bbtweaks.durability_warnings.config;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DurabilityWarningCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "durabilitywarning";

  public DurabilityWarningCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

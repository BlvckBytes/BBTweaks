package me.blvckbytes.bbtweaks.hotbar_randomizer.command;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class HotbarRandomizerCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "hotbarrandomizer";

  public HotbarRandomizerCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

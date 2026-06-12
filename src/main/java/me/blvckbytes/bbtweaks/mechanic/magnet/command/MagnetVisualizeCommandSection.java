package me.blvckbytes.bbtweaks.mechanic.magnet.command;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MagnetVisualizeCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "mvisualize";

  public MagnetVisualizeCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

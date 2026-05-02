package me.blvckbytes.bbtweaks.inv_magnet.config;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InvMagnetCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "invmagnet";

  public InvMagnetCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

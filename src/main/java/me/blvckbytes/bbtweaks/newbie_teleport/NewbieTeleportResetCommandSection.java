package me.blvckbytes.bbtweaks.newbie_teleport;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class NewbieTeleportResetCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "newbieteleportreset";

  public NewbieTeleportResetCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

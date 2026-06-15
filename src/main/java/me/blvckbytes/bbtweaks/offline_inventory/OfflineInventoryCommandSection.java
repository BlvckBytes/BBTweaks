package me.blvckbytes.bbtweaks.offline_inventory;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class OfflineInventoryCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "offlineinventory";

  public OfflineInventoryCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

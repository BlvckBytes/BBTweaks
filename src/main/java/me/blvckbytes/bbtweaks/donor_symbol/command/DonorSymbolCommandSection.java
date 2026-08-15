package me.blvckbytes.bbtweaks.donor_symbol.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DonorSymbolCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "donorsymbol";

  public ComponentMarkup playersOnly;
  public ComponentMarkup noPermission;

  public DonorSymbolCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

package me.blvckbytes.bbtweaks.item_piling.command;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ItemPilingCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "itempiling";

  public ComponentMarkup playersOnly;

  public ItemPilingCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}

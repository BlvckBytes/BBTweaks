package me.blvckbytes.bbtweaks.inv_filter.display;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InvFilterDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection enabled;
  public GuiItemStackSection help;
  public GuiItemStackSection filterSlot;

  public InvFilterDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

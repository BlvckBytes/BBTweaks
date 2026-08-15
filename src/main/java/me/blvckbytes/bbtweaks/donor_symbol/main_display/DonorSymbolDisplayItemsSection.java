package me.blvckbytes.bbtweaks.donor_symbol.main_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class DonorSymbolDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection enabled;
  public GuiItemStackSection symbol;
  public GuiItemStackSection color;
  public GuiItemStackSection info;

  public DonorSymbolDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

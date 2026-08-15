package me.blvckbytes.bbtweaks.donor_symbol.main_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DonorSymbolDisplaySection extends GuiSection<DonorSymbolDisplayItemsSection> {

  public DonorSymbolDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(DonorSymbolDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

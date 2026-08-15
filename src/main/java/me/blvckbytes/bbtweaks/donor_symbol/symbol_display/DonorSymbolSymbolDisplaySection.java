package me.blvckbytes.bbtweaks.donor_symbol.symbol_display;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.gui.PaginatedGuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DonorSymbolSymbolDisplaySection extends PaginatedGuiSection<DonorSymbolSymbolDisplayItemsSection> {

  public ComponentMarkup symbolSelected;
  public ComponentMarkup symbolAlreadySelected;

  public DonorSymbolSymbolDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(DonorSymbolSymbolDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

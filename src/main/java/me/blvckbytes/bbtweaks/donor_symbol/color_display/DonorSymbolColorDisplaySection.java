package me.blvckbytes.bbtweaks.donor_symbol.color_display;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.gui.PaginatedGuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class DonorSymbolColorDisplaySection extends PaginatedGuiSection<DonorSymbolColorDisplayItemsSection> {

  public ComponentMarkup colorSelected;
  public ComponentMarkup colorAlreadySelected;

  public DonorSymbolColorDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(DonorSymbolColorDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

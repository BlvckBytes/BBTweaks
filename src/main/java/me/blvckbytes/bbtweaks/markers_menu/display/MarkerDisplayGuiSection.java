package me.blvckbytes.bbtweaks.markers_menu.display;

import at.blvckbytes.cm_mapper.section.gui.PaginatedGuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MarkerDisplayGuiSection extends PaginatedGuiSection<MarkerDisplayItemsSection> {

  public MarkerDisplayGuiSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(MarkerDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

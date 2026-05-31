package me.blvckbytes.bbtweaks.sidebar.sorting_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SidebarSortingDisplaySection extends GuiSection<SidebarSortingDisplayItemsSection> {

  public SidebarSortingDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(SidebarSortingDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

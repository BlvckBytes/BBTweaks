package me.blvckbytes.bbtweaks.markers_menu.display;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MarkerDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection previousPage;
  public GuiItemStackSection backToCategoriesButton;
  public GuiItemStackSection nextPage;
  public GuiItemStackSection filler;

  public MarkerDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

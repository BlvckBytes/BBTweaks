package me.blvckbytes.bbtweaks.item_piling.display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ItemPilingDisplaySection extends GuiSection<ItemPilingDisplayItemsSection> {

  public ItemPilingDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(ItemPilingDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

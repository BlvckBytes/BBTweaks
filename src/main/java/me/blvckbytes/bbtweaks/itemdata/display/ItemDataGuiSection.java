package me.blvckbytes.bbtweaks.itemdata.display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ItemDataGuiSection extends GuiSection<ItemDataGuiItemsSection> {

  public ItemDataGuiSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(ItemDataGuiItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

package me.blvckbytes.bbtweaks.sidebar.color_display;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class ColorDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection backButton;
  public GuiItemStackSection labelColorMode;
  public GuiItemStackSection valueColorMode;
  public GuiItemStackSection toggleBold;
  public GuiItemStackSection toggleItalic;
  public GuiItemStackSection toggleUnderlined;
  public ItemStackSection colorIcon;

  public ColorDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

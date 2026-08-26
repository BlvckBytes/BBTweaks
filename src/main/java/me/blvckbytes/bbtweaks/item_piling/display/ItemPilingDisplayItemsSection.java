package me.blvckbytes.bbtweaks.item_piling.display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class ItemPilingDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;

  public GuiItemStackSection itemCountIcon;
  public GuiItemStackSection itemMaterialIcon;

  public GuiItemStackSection unitStackIcon;
  public GuiItemStackSection vanillaStackIcon;
  public GuiItemStackSection piledStackIcon;

  public GuiItemStackSection showItemCountForUnitStacks;
  public GuiItemStackSection showItemCountForVanillaStacks;
  public GuiItemStackSection showItemCountForPiledStacks;

  public GuiItemStackSection showItemMaterialForUnitStacks;
  public GuiItemStackSection showItemMaterialForVanillaStacks;
  public GuiItemStackSection showItemMaterialForPiledStacks;

  public GuiItemStackSection formatItemCountToStacks;
  public GuiItemStackSection immediatelyStackBlockBreakItems;

  public ItemPilingDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

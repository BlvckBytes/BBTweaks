package me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.item.ItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class HotbarRandomizerSettingsDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection enabled;
  public GuiItemStackSection enabledSlot;
  public GuiItemStackSection hotbarItemPatch;
  public ItemStackSection emptySlotPlaceholder;

  public HotbarRandomizerSettingsDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

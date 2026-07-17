package me.blvckbytes.bbtweaks.block_facing.settings_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class BlockFacingSettingsDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection enabled;
  public GuiItemStackSection facing;
  public GuiItemStackSection filler;

  public BlockFacingSettingsDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

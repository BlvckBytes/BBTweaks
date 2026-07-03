package me.blvckbytes.bbtweaks.block_facing.settings_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class BlockFacingSettingsDisplaySection extends GuiSection<BlockFacingSettingsDisplayItemsSection > {

  public BlockFacingSettingsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(BlockFacingSettingsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

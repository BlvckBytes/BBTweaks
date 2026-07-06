package me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class HotbarRandomizerSettingsDisplaySection extends GuiSection<HotbarRandomizerSettingsDisplayItemsSection> {

  public HotbarRandomizerSettingsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(HotbarRandomizerSettingsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

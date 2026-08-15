package me.blvckbytes.bbtweaks.emotions.settings_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class EmotionSettingsDisplaySection extends GuiSection<EmotionSettingsDisplayItemsSection> {

  public EmotionSettingsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(EmotionSettingsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

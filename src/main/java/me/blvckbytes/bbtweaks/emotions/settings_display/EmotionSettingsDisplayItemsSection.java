package me.blvckbytes.bbtweaks.emotions.settings_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class EmotionSettingsDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;

  public GuiItemStackSection information;

  public GuiItemStackSection descriptionChatPart;
  public GuiItemStackSection descriptionActionBarPart;
  public GuiItemStackSection descriptionTitlePart;
  public GuiItemStackSection descriptionSoundPart;
  public GuiItemStackSection descriptionEffectPart;

  public GuiItemStackSection descriptionAllOrigin;
  public GuiItemStackSection descriptionDirectOrigin;
  public GuiItemStackSection descriptionIsSenderOrigin;
  public GuiItemStackSection descriptionBroadcastOrigin;

  public GuiItemStackSection statusButtons;

  public EmotionSettingsDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

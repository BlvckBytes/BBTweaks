package me.blvckbytes.bbtweaks.sign_copier.settings_display;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class SignCopierSettingsDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection filler;
  public GuiItemStackSection pasteSignColor;
  public GuiItemStackSection pasteSignGlowing;
  public GuiItemStackSection sendCopiedMessage;
  public GuiItemStackSection sendPastedMessage;

  public SignCopierSettingsDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

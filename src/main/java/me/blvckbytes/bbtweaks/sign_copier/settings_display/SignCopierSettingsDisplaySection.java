package me.blvckbytes.bbtweaks.sign_copier.settings_display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SignCopierSettingsDisplaySection extends GuiSection<SignCopierSettingsDisplayItemsSection > {

  public SignCopierSettingsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(SignCopierSettingsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

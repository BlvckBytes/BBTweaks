package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.section.gui.PaginatedGuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SettingsDisplaySection extends PaginatedGuiSection<SettingsDisplayItemsSection> {

  public SettingsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(SettingsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

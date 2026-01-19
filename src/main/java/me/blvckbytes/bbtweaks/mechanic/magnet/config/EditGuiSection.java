package me.blvckbytes.bbtweaks.mechanic.magnet.config;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class EditGuiSection extends GuiSection<EditDisplayItemsSection> {

  public EditGuiSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(EditDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

package me.blvckbytes.bbtweaks.inv_filter.display;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InvFilterDisplaySection extends GuiSection<InvFilterDisplayItemsSection> {

  public InvFilterDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(InvFilterDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

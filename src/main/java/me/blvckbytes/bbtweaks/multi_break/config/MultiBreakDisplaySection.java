package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MultiBreakDisplaySection extends GuiSection<MultiBreakDisplayItemsSection> {

  public MultiBreakDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(MultiBreakDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}

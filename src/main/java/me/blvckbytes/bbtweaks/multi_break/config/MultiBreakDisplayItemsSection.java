package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class MultiBreakDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection extentLeft;
  public GuiItemStackSection extentRight;
  public GuiItemStackSection extentUp;
  public GuiItemStackSection extentDown;
  public GuiItemStackSection extentDepth;

  public GuiItemStackSection currentFilter;
  public GuiItemStackSection sneakMode;
  public GuiItemStackSection toggleEnabled;

  public GuiItemStackSection filler;

  public MultiBreakDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

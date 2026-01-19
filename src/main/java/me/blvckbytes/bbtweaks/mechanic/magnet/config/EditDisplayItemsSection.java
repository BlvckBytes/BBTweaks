package me.blvckbytes.bbtweaks.mechanic.magnet.config;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class EditDisplayItemsSection extends ConfigSection {

  public GuiItemStackSection selectParameterExtentX;
  public GuiItemStackSection selectParameterExtentY;
  public GuiItemStackSection selectParameterExtentZ;
  public GuiItemStackSection selectParameterOffsetX;
  public GuiItemStackSection selectParameterOffsetY;
  public GuiItemStackSection selectParameterOffsetZ;

  public GuiItemStackSection filler;
  public GuiItemStackSection save;
  public GuiItemStackSection cancel;

  public EditDisplayItemsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

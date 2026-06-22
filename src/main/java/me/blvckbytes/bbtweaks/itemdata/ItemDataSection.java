package me.blvckbytes.bbtweaks.itemdata;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.itemdata.display.ItemDataGuiSection;

public class ItemDataSection extends ConfigSection {

  public @CSAlways ItemDataCommandSection command;

  public ComponentMarkup playersOnly;
  public ComponentMarkup noItemInMainHand;
  public ComponentMarkup commandUsage;
  public ComponentMarkup noItemInInventory;
  public ComponentMarkup heldItemHasNoData;
  public ComponentMarkup heldItemScreen;
  public ComponentMarkup cannotModifyDisplay;

  public @CSAlways ItemDataGuiSection infoDisplay;

  public ItemDataSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

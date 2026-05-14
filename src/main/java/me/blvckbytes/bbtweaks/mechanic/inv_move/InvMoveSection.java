package me.blvckbytes.bbtweaks.mechanic.inv_move;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InvMoveSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup noContainer;
  public ComponentMarkup existingSign;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup noItemsMatchingFilterInInventory;
  public ComponentMarkup noItemsInInventory;
  public ComponentMarkup targetInventoryIsFull;
  public ComponentMarkup unloadProcessCompleted;

  public InvMoveSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

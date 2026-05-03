package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class QuickUnloadSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup noContainer;
  public ComponentMarkup existingSign;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup noContainerInMainHand;
  public ComponentMarkup emptyContainerInMainHand;
  public ComponentMarkup noContainerInInventory;
  public ComponentMarkup allContainersInInventoryAreEmpty;
  public ComponentMarkup targetInventoryIsFull;
  public ComponentMarkup unloadProcessCompleted;

  public QuickUnloadSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

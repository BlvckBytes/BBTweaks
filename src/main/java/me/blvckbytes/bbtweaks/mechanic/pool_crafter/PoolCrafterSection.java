package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class PoolCrafterSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup notOnADropper;
  public ComponentMarkup existingSign;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup cannotEditSign;
  public ComponentMarkup selectedRecipeResults;

  public PoolCrafterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

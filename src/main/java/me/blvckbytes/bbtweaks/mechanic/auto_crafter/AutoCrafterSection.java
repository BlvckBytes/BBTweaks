package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AutoCrafterSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup notOnACrafter;
  public ComponentMarkup existingSign;
  public ComponentMarkup unknownFlag;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup cannotEditSign;
  public ComponentMarkup anotherIsEditing;
  public ComponentMarkup metaRecipeInventoryOpening;
  public ComponentMarkup metaRecipeInventoryTitle;
  public ComponentMarkup metaRecipeInventorySaved;

  public AutoCrafterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

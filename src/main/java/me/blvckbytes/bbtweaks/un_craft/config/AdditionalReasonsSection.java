package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class AdditionalReasonsSection extends ConfigSection {

  public ComponentMarkup hasDamage;
  public ComponentMarkup hasName;
  public ComponentMarkup hasLore;
  public ComponentMarkup hasEnchants;
  public ComponentMarkup hasAttributeModifiers;
  public ComponentMarkup hasPdcKeys;
  public ComponentMarkup hasInnerItems;
  public ComponentMarkup noEntryFound;
  public ComponentMarkup noReasonGiven;
  public ComponentMarkup recoloringRecipe;
  public ComponentMarkup smithingRecipe;

  public AdditionalReasonsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

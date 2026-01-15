package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.ArrayList;
import java.util.List;

public class UnCraftSection extends ConfigSection {

  public ComponentMarkup inGameOnly;
  public ComponentMarkup missingPermission;
  public ComponentMarkup missingPermissionAllMode;
  public ComponentMarkup noItemInMainHand;
  public ComponentMarkup unsupportedItem;
  public ComponentMarkup choicesScreen;
  public ComponentMarkup invalidSelection;
  public ComponentMarkup unacceptedSubtraction;
  public ComponentMarkup notEnoughItems;
  public ComponentMarkup notEnoughItemsReduced;
  public ComponentMarkup droppedItems;
  public ComponentMarkup noMoreSpace;
  public ComponentMarkup notEnoughSpace;
  public ComponentMarkup itemOverview;
  public ComponentMarkup successfulUnCraft;

  public @CSAlways AdditionalReasonsSection additionalReasons;

  public List<TypeExclusionRule> typeExclusionRules = new ArrayList<>();
  public List<IOTypeRule> typeInclusionRules = new ArrayList<>();
  public List<ResultSubtractionRule> resultSubtractionRules = new ArrayList<>();
  public List<RecipeExclusionRule> recipeExclusionRules = new ArrayList<>();
  public List<AdditionalRecipe> additionalRecipes = new ArrayList<>();
  public List<PreferredMaterial> preferredMaterials = new ArrayList<>();

  public UnCraftSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

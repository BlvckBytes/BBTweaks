package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.util.*;

public class AdditionalRecipesSection extends ConfigSection {

  public Map<String, ShapedRecipeSection> shapedRecipes = new HashMap<>();

  public Map<String, ShapelessRecipeSection> shapelessRecipes = new HashMap<>();

  public AdditionalRecipesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}

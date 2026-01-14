package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.ParsedRecipe;
import me.blvckbytes.bbtweaks.un_craft.RecipeSyntax;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeExclusionRule extends ConfigSection {

  public List<String> recipes;

  @CSIgnore
  public List<ParsedRecipe> _recipes = new ArrayList<>();

  public String reason;

  public RecipeExclusionRule(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var recipe : recipes) {
      try {
        _recipes.add(RecipeSyntax.tryParseRecipe(recipe));
      } catch (Throwable e) {
        throw new MappingError("Could not parse recipe-syntax \"" + recipe + "\": " + e.getMessage());
      }
    }
  }

  public boolean matches(
    Material uncraftedItemType,
    int uncraftedItemAmount,
    Map<Material, Integer> uncraftResults
  ) {
    for (var recipe : _recipes) {
      if (recipe.uncraftedItemType() != uncraftedItemType)
        continue;

      if (recipe.uncraftedItemAmount() != uncraftedItemAmount)
        continue;

      if (!recipe.uncraftResults().equals(uncraftResults))
        continue;

      return true;
    }

    return false;
  }
}

package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

public class AdditionalRecipesSection extends ConfigSection {

  // Yes, this is a bit nasty, but I need to move on now...
  public static @Nullable Plugin plugin;

  public Map<String, ShapedRecipeSection> shapedRecipes = new HashMap<>();

  @CSIgnore
  public List<ShapedRecipe> _shapedRecipes = new ArrayList<>();

  public AdditionalRecipesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (plugin == null)
      throw new IllegalStateException("The static plugin-reference has not been set!");

    recipeLoop: for (var shapedRecipeEntry : shapedRecipes.entrySet()) {
      var recipeName = new NamespacedKey(plugin, shapedRecipeEntry.getKey());
      var recipeSection = shapedRecipeEntry.getValue();
      var shapedRecipe = new ShapedRecipe(recipeName, new ItemStack(recipeSection.result, recipeSection.amount));

      var shapeLines = recipeSection.shape.stream().map(it -> it.asPlainString(null)).toList();

      if (shapeLines.size() != 3) {
        plugin.getLogger().warning("Shape for recipe \"" + recipeName + "\" has " + shapeLines.size() + " rows instead of three");
        continue;
      }

      var requiredIngredients = new HashSet<Character>();

      for (var shapeLine : shapeLines) {
        if (shapeLine.length() != 3) {
          plugin.getLogger().warning("Malformed shape for recipe \"" + recipeName + "\": shape-lines must be exactly three chars long");
          continue recipeLoop;
        }

        for (var index = 0; index < 3; ++index) {
          var c = shapeLine.charAt(index);

          if (c >= 'A' && c <= 'Z') {
            requiredIngredients.add(c);
            continue;
          }

          plugin.getLogger().warning("Malformed shape for recipe \"" + recipeName + "\": shape-characters must only be A-Z");
          continue recipeLoop;
        }
      }

      shapedRecipe.shape(shapeLines.toArray(String[]::new));

      if (recipeSection.ingredients == null) {
        plugin.getLogger().warning("Recipe \"" + recipeName + "\" misses \"ingredients\"");
        continue;
      }

      for (var ingredientName : recipeSection.ingredients.keySet()) {
        char c;

        if (ingredientName.length() != 1 || (c = ingredientName.charAt(0)) < 'A' || c > 'Z') {
          plugin.getLogger().warning("Recipe \"" + recipeName + "\" has invalid ingredient \"" + ingredientName + "\": must only be a single char A-Z");
          continue recipeLoop;
        }

        if (!requiredIngredients.remove(c)) {
          plugin.getLogger().warning("Recipe \"" + recipeName + "\" has unused ingredient \"" + c + "\"");
          continue recipeLoop;
        }

        var ingredient = recipeSection.ingredients.get(ingredientName);

        shapedRecipe.setIngredient(c, ingredient._choice);
      }

      if (!requiredIngredients.isEmpty()) {
        plugin.getLogger().warning("Missing ingredient-type \"" + requiredIngredients.iterator().next() + "\" for recipe \"" + recipeName + "\"");
        continue;
      }

      _shapedRecipes.add(shapedRecipe);
    }
  }
}

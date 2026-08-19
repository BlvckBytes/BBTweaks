package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShapedRecipeSection extends RecipeSection {

  public List<String> shape;
  public Map<String, IngredientSection> ingredients;

  public ShapedRecipeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public @Nullable ShapedRecipe buildRecipe(Plugin plugin, String name) {
    var recipeName = new NamespacedKey(plugin, name);

    if (shape == null) {
      plugin.getLogger().warning("Recipe \"" + recipeName + "\" misses \"shape\"");
      return null;
    }

    var shapedRecipe = new ShapedRecipe(recipeName, new ItemStack(result, amount));

    if (shape.size() < 2) {
      plugin.getLogger().warning("Shape for recipe \"" + recipeName + "\" has less than two lines");
      return null;
    }

    var requiredIngredients = new HashSet<Character>();

    for (var shapeLine : shape) {
      if (shapeLine.length() < 2) {
        plugin.getLogger().warning("Malformed shape for recipe \"" + recipeName + "\": less than two shape-lines encountered");
        return null;
      }

      for (var index = 0; index < shapeLine.length(); ++index) {
        var c = shapeLine.charAt(index);

        if (c >= 'A' && c <= 'Z') {
          requiredIngredients.add(c);
          continue;
        }

        plugin.getLogger().warning("Malformed shape for recipe \"" + recipeName + "\": shape-characters must only be A-Z");
        return null;
      }
    }

    shapedRecipe.shape(shape.toArray(String[]::new));

    if (ingredients == null) {
      plugin.getLogger().warning("Recipe \"" + recipeName + "\" misses \"ingredients\"");
      return null;
    }

    for (var ingredientName : ingredients.keySet()) {
      char c;

      if (ingredientName.length() != 1 || (c = ingredientName.charAt(0)) < 'A' || c > 'Z') {
        plugin.getLogger().warning("Recipe \"" + recipeName + "\" has invalid ingredient \"" + ingredientName + "\": must only be a single char A-Z");
        return null;
      }

      if (!requiredIngredients.remove(c)) {
        plugin.getLogger().warning("Recipe \"" + recipeName + "\" has unused ingredient \"" + c + "\"");
        return null;
      }

      var ingredient = ingredients.get(ingredientName);

      shapedRecipe.setIngredient(c, ingredient._choice);
    }

    if (!requiredIngredients.isEmpty()) {
      plugin.getLogger().warning("Missing ingredient-type \"" + requiredIngredients.iterator().next() + "\" for recipe \"" + recipeName + "\"");
      return null;
    }

    return shapedRecipe;
  }
}

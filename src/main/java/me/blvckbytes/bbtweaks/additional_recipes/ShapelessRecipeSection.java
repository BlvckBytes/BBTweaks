package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShapelessRecipeSection extends RecipeSection {

  public List<IngredientSection> ingredients;

  public ShapelessRecipeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public @Nullable ShapelessRecipe buildRecipe(Plugin plugin, String name) {
    var recipeName = new NamespacedKey(plugin, name);

    if (ingredients == null) {
      plugin.getLogger().warning("Recipe \"" + recipeName + "\" misses \"ingredients\"");
      return null;
    }

    var shapelessRecipe = new ShapelessRecipe(recipeName, new ItemStack(result, amount));

    if (ingredients.isEmpty()) {
      plugin.getLogger().warning("Recipe \"" + recipeName + "\" has empty \"ingredients\"");
      return null;
    }

    for (var ingredient : ingredients)
      shapelessRecipe.addIngredient(ingredient._choice);

    return shapelessRecipe;
  }
}

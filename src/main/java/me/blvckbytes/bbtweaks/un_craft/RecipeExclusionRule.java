package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record RecipeExclusionRule(
  List<ParsedRecipe> recipes,
  String reason
) {
  public boolean matches(
    Material uncraftedItemType,
    int uncraftedItemAmount,
    Map<Material, Integer> uncraftResults
  ) {
    for (var recipe : recipes) {
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

  public static RecipeExclusionRule fromConfig(ConfigurationSection section) {
    var recipeStrings = section.getStringList("recipes");
    var recipes = new ArrayList<ParsedRecipe>();

    for (var recipeString : recipeStrings) {
      try {
        recipes.add(RecipeSyntax.tryParseRecipe(recipeString));
      } catch (Throwable e) {
        throw new IllegalStateException("Could not parse recipe-syntax \"" + recipeString + "\": " + e.getMessage());
      }
    }

    var reason = section.getString("reason");

    if (reason == null || (reason = reason.trim()).isEmpty())
      throw new IllegalStateException("Missing a non-blank reason!");

    return new RecipeExclusionRule(recipes, reason);
  }
}

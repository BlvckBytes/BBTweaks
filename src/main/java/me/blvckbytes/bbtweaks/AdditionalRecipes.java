package me.blvckbytes.bbtweaks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class AdditionalRecipes {

  private final BBTweaksPlugin plugin;
  private final List<NamespacedKey> recipeKeys;

  public AdditionalRecipes(BBTweaksPlugin plugin) {
    this.plugin = plugin;
    this.recipeKeys = new ArrayList<>();

    plugin.registerConfigReloadListener(this::updateRecipesFromConfig);
    updateRecipesFromConfig();
  }

  private void updateRecipesFromConfig() {
    removeRegisteredRecipes();
    addRecipesFromConfig();

    plugin.getLogger().info("Loaded " + recipeKeys.size() + " custom recipes");
  }

  private void addRecipesFromConfig() {
    var recipeMap = plugin.getConfiguration().getConfigurationSection("additionalShapedRecipes");

    if (recipeMap == null)
      return;

    var registeredNames = new HashSet<String>();

    recipeLoop: for (var recipeName : recipeMap.getKeys(false)) {
      var recipeSection = recipeMap.getConfigurationSection(recipeName);

      if (recipeSection == null)
        continue;

      if (!registeredNames.add(recipeName.toLowerCase())) {
        plugin.getLogger().warning("Duplicate additional recipe \"" + recipeName + "\"");
        continue;
      }

      var resultTypeString = recipeSection.getString("result", "BARRIER");
      Material resultType;

      try {
        resultType = Material.valueOf(resultTypeString.toUpperCase().trim());
      } catch (Throwable e) {
        plugin.getLogger().warning("Malformed result \"" + resultTypeString + "\" for recipe \"" + recipeName + "\"");
        continue;
      }

      var amountString = recipeSection.getString("amount", "1");
      int resultAmount;

      try {
        resultAmount = Integer.parseInt(amountString);

        if (resultAmount <= 0)
          throw new IllegalStateException();
      } catch (Throwable e) {
        plugin.getLogger().warning("Malformed amount \"" + amountString + "\" for recipe \"" + recipeName + "\"");
        continue;
      }

      var result = new ItemStack(resultType, resultAmount);
      var nameKey = new NamespacedKey(plugin, recipeName);

      var recipe = new ShapedRecipe(nameKey, result);

      var shapeLines = recipeSection.getStringList("shape");

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

      recipe.shape(shapeLines.toArray(String[]::new));

      var ingredientMap = recipeSection.getConfigurationSection("ingredients");

      if (ingredientMap == null) {
        plugin.getLogger().warning("Recipe \"" + recipeName + "\" misses \"ingredients\"");
        continue;
      }

      for (var ingredientName : ingredientMap.getKeys(false)) {
        char c;

        if (ingredientName.length() != 1 || (c = ingredientName.charAt(0)) < 'A' || c > 'Z') {
          plugin.getLogger().warning("Recipe \"" + recipeName + "\" has invalid ingredient \"" + ingredientName + "\": must only be a single char A-Z");
          continue recipeLoop;
        }

        if (!requiredIngredients.remove(c)) {
          plugin.getLogger().warning("Recipe \"" + recipeName + "\" has unused ingredient \"" + c + "\"");
          continue recipeLoop;
        }

        var ingredient = ingredientMap.getConfigurationSection(ingredientName);

        if (ingredient == null)
          continue;

        var ingredientTypeString = ingredient.getString("type", "BARRIER");
        Material ingredientType;

        try {
          ingredientType = Material.valueOf(ingredientTypeString.toUpperCase().trim());
        } catch (Throwable e) {
          plugin.getLogger().warning("Malformed ingredient-type \"" + ingredientTypeString + "\" for recipe \"" + recipeName + "\"");
          continue recipeLoop;
        }

        recipe.setIngredient(c, ingredientType);
      }

      if (!requiredIngredients.isEmpty()) {
        plugin.getLogger().warning("Missing ingredient-type \"" + requiredIngredients.iterator().next() + "\" for recipe \"" + recipeName + "\"");
        continue;
      }

      Bukkit.addRecipe(recipe);
      recipeKeys.add(nameKey);
    }
  }

  private void removeRegisteredRecipes() {
    for (var keyIterator = recipeKeys.iterator(); keyIterator.hasNext();) {
      var key = keyIterator.next();
      Bukkit.removeRecipe(key);
      keyIterator.remove();
    }
  }
}

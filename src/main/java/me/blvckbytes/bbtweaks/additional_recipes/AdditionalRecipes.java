package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AdditionalRecipes {

  private final Logger logger;
  private final ConfigKeeper<MainSection> config;
  private final List<NamespacedKey> recipeKeys;

  public AdditionalRecipes(Logger logger, ConfigKeeper<MainSection> config) {
    this.logger = logger;
    this.config = config;
    this.recipeKeys = new ArrayList<>();

    config.registerReloadListener(this::updateRecipesFromConfig);
    updateRecipesFromConfig();
  }

  private void updateRecipesFromConfig() {
    removeRegisteredRecipes();
    addRecipesFromConfig();

    logger.info("Loaded " + recipeKeys.size() + " custom recipes");
  }

  private void addRecipesFromConfig() {
    for (var shapedRecipe : config.rootSection.additionalRecipes._shapedRecipes) {
      Bukkit.addRecipe(shapedRecipe);
      recipeKeys.add(shapedRecipe.getKey());
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

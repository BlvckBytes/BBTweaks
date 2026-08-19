package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class AdditionalRecipes implements Listener {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final List<NamespacedKey> recipeKeys;

  public AdditionalRecipes(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;
    this.recipeKeys = new ArrayList<>();

    updateRecipesFromConfig();
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper == config)
      updateRecipesFromConfig();
  }

  private void updateRecipesFromConfig() {
    removeRegisteredRecipes();
    addRecipesFromConfig();

    plugin.getLogger().info("Loaded " + recipeKeys.size() + " custom recipes");
  }

  private void addRecipesFromConfig() {
    for (var shapedRecipeEntry : config.rootSection.additionalRecipes.shapedRecipes.entrySet()) {
      var shapedRecipe = shapedRecipeEntry.getValue().buildRecipe(plugin, shapedRecipeEntry.getKey());

      if (shapedRecipe == null)
        continue;

      if (!Bukkit.addRecipe(shapedRecipe)) {
        plugin.getLogger().warning("Could not add shaped recipe " + shapedRecipe.getKey() + " - is it conflicting with existing recipes?");
        continue;
      }

      recipeKeys.add(shapedRecipe.getKey());
    }

    for (var shapelessRecipeEntry : config.rootSection.additionalRecipes.shapelessRecipes.entrySet()) {
      var shapelessRecipe = shapelessRecipeEntry.getValue().buildRecipe(plugin, shapelessRecipeEntry.getKey());

      if (shapelessRecipe == null)
        continue;

      if (!Bukkit.addRecipe(shapelessRecipe)) {
        plugin.getLogger().warning("Could not add shapeless recipe " + shapelessRecipe.getKey() + " - is it conflicting with existing recipes?");
        continue;
      }

      recipeKeys.add(shapelessRecipe.getKey());
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

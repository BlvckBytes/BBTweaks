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

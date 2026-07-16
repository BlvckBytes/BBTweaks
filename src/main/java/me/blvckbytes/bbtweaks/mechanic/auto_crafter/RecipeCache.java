package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.Material;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;

public interface RecipeCache {

  List<CachedRecipe> getRecipes();

  RecipeChoice.MaterialChoice expandMetaMaterial(Material material);

}

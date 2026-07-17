package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RecipeCache {

  List<CachedRecipe> getRecipes();

  @Nullable Material getEmptyTypeAfterUse(Material ingredient);

}

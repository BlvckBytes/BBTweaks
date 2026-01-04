package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.*;

public class UnCraftRecipeMap {

  private final Map<Material, List<UnCraftEntry>> recipesByUnCraftedType;

  public UnCraftRecipeMap() {
    this.recipesByUnCraftedType = new HashMap<>();
  }

  public Set<Map.Entry<Material, List<UnCraftEntry>>> entrySet() {
    return recipesByUnCraftedType.entrySet();
  }

  public List<UnCraftEntry> getRecipesFor(Material unCraftedType) {
    var recipes = recipesByUnCraftedType.get(unCraftedType);

    if (recipes == null)
      return Collections.emptyList();

    return recipes;
  }

  public void addUnCraftingRecipe(Material unCraftedType, UnCraftEntry recipe) {
    recipesByUnCraftedType.computeIfAbsent(unCraftedType, k -> new ArrayList<>()).add(recipe);
  }

  public void clear() {
    recipesByUnCraftedType.clear();
  }
}

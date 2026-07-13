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

  public boolean addUnCraftingRecipe(Material unCraftedType, UnCraftEntry recipe) {
    var bucket = recipesByUnCraftedType.computeIfAbsent(unCraftedType, _ -> new ArrayList<>());

    // Allow to have multiple excluded recipes for the same type, seeing how we do accumulate
    // various reasons as to why an input is denied, but do not store multiple usable recipes
    // that are equivalent, since that always represents (unintentional) duplication.
    if (recipe.exclusionReasons.isEmpty()) {
      for (var entry : bucket) {
        if (!entry.exclusionReasons.isEmpty())
          continue;

        if (entry.matchesResultTypes(recipe))
          return false;
      }
    }

    bucket.add(recipe);
    return true;
  }

  public void clear() {
    recipesByUnCraftedType.clear();
  }
}

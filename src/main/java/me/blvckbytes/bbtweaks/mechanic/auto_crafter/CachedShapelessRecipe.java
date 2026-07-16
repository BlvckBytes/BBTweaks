package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CachedShapelessRecipe implements CachedRecipe {

  private final ItemStack result;
  private final List<RecipeChoice.MaterialChoice> choiceList;
  private final NamespacedKey key;

  private CachedShapelessRecipe(
    ItemStack result,
    List<RecipeChoice.MaterialChoice> choiceList,
    NamespacedKey key
  ) {
    this.result = result;
    this.choiceList = choiceList;
    this.key = key;
  }

  public static @Nullable CachedShapelessRecipe createIfValid(ShapelessRecipe recipe) {
    var choiceList = new ArrayList<RecipeChoice.MaterialChoice>();

    for (var recipeChoice : recipe.getChoiceList()) {
      if (recipeChoice == null || RecipeChoice.empty().equals(recipeChoice))
        continue;

      if (!(recipeChoice instanceof RecipeChoice.MaterialChoice materialChoice))
        return null;

      choiceList.add(materialChoice);
    }

    if (choiceList.isEmpty())
      return null;

    return new CachedShapelessRecipe(recipe.getResult(), choiceList, recipe.getKey());
  }

  @Override
  public ItemStack getResultCopy() {
    return new ItemStack(result);
  }

  @Override
  public NamespacedKey getKey() {
    return key;
  }

  public List<RecipeChoice.MaterialChoice> getChoiceList() {
    return Collections.unmodifiableList(choiceList);
  }
}

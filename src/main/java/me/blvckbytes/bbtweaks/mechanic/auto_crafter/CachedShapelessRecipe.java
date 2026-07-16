package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

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

  private CachedShapelessRecipe(
    ItemStack result,
    List<RecipeChoice.MaterialChoice> choiceList
  ) {
    this.result = result;
    this.choiceList = choiceList;
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

    return new CachedShapelessRecipe(recipe.getResult(), choiceList);
  }

  @Override
  public ItemStack getResultCopy() {
    return new ItemStack(result);
  }

  public List<RecipeChoice> getChoiceList() {
    return Collections.unmodifiableList(choiceList);
  }
}

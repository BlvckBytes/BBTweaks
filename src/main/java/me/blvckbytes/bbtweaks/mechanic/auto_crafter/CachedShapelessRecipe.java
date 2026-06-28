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
  private final List<RecipeChoice> choiceList;

  private CachedShapelessRecipe(
    ItemStack result,
    List<RecipeChoice> choiceList
  ) {
    this.result = result;
    this.choiceList = choiceList;
  }

  public static @Nullable CachedShapelessRecipe createIfValid(ShapelessRecipe recipe) {
    var choiceList = new ArrayList<RecipeChoice>();

    for (var recipeChoice : recipe.getChoiceList()) {
      if (recipeChoice == null || recipeChoice == RecipeChoice.empty())
        continue;

      choiceList.add(recipeChoice);
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

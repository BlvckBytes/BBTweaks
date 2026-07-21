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

    return new CachedShapelessRecipe(
      recipe.getResult(),
      Collections.unmodifiableList(choiceList)
    );
  }

  @Override
  public ItemStack getResultCopy() {
    return new ItemStack(result);
  }

  @Override
  public boolean areMatrixContentsSatisfyingRecipe(MatrixContent[] matrixContents) {
    var remainingIngredients = new ArrayList<>(choiceList);

    // If it's empty already, something is wrong with the recipe.
    if (remainingIngredients.isEmpty())
      return false;

    contentLoop:
    for (var matrixContent : matrixContents) {
      if (!matrixContent.isPresent())
        continue;

      // No more required ingredients left, but there are still additional items in the crafting-matrix => mismatch.
      if (remainingIngredients.isEmpty())
        return false;

      for (var iterator = remainingIngredients.iterator(); iterator.hasNext();) {
        var requiredChoice = iterator.next();

        if (matrixContent.test(requiredChoice)) {
          iterator.remove();
          continue contentLoop;
        }
      }

      // The current matrix-content found no remaining ingredient, so we're dealing with an excess.
      return false;
    }

    return remainingIngredients.isEmpty();
  }

  @Override
  public List<RecipeChoice.MaterialChoice> getChoicesForAllSlots() {
    return choiceList;
  }
}

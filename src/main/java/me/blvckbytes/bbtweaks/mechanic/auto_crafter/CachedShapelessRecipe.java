package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

  @Override
  public <T> boolean areMatrixContentsSatisfyingRecipe(T[] matrixContents, Function<T, MatrixContent> contentMapper) {
    var remainingIngredients = new ArrayList<>(choiceList);

    // If it's empty already, something is wrong with the recipe.
    if (remainingIngredients.isEmpty())
      return false;

    for (var matrixContent : matrixContents) {
      var mappedMatrixContent = contentMapper.apply(matrixContent);

      if (!mappedMatrixContent.isPresent())
        continue;

      // No more required ingredients left, but there are still additional items in the crafting-matrix => mismatch.
      if (remainingIngredients.isEmpty())
        return false;

      for (var iterator = remainingIngredients.iterator(); iterator.hasNext();) {
        var requiredChoice = iterator.next();

        if (mappedMatrixContent.test(requiredChoice)) {
          iterator.remove();
          break;
        }
      }
    }

    return remainingIngredients.isEmpty();
  }
}

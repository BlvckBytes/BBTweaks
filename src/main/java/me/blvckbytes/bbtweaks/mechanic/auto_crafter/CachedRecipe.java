package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;

public interface CachedRecipe {

  ItemStack getResultCopy();

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  boolean areMatrixContentsSatisfyingRecipe(MatrixContent[] matrixContents);

  List<RecipeChoice.MaterialChoice> getChoicesForAllSlots();

}

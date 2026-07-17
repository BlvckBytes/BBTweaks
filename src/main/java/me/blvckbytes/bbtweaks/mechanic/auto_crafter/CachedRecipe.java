package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public interface CachedRecipe {

  ItemStack getResultCopy();

  NamespacedKey getKey();

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  <T> boolean areMatrixContentsSatisfyingRecipe(T[] matrixContents, Function<T, MatrixContent> contentMapper);

}

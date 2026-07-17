package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.Nullable;

public class MatrixItem implements MatrixContent {

  private final @Nullable ItemStack itemStack;

  private MatrixItem(@Nullable ItemStack itemStack) {
    this.itemStack = itemStack;
  }

  public static MatrixItem[] map(ItemStack[] matrixContents) {
    var mappedContents = new MatrixItem[matrixContents.length];

    for (var index = 0; index < mappedContents.length; ++index)
      mappedContents[index] = new MatrixItem(matrixContents[index]);

    return mappedContents;
  }

  @Override
  public boolean test(RecipeChoice.MaterialChoice materialChoice) {
    if (itemStack == null)
      return false;

    return materialChoice.test(itemStack);
  }

  @Override
  public boolean isPresent() {
    return ItemUtil.isStackValid(itemStack);
  }
}

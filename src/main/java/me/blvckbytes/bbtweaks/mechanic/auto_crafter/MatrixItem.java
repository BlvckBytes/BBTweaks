package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.Nullable;

public class MatrixItem implements MatrixContent {

  private final @Nullable ItemStack itemStack;

  public MatrixItem(@Nullable ItemStack itemStack) {
    this.itemStack = itemStack;
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

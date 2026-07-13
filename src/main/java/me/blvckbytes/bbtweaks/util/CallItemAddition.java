package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;

public record CallItemAddition(ItemStack addedItem, int addedAmount, int stackSizeOverride) {

  public CallItemAddition(ItemStack addedItem, int addedAmount) {
    this(addedItem, addedAmount, 0);
  }

  public boolean isSimilarWithEqualParameters(CallItemAddition other) {
    if (!other.addedItem.isSimilar(this.addedItem))
      return false;

    return addedAmount == other.addedAmount && stackSizeOverride == other.stackSizeOverride;
  }
}

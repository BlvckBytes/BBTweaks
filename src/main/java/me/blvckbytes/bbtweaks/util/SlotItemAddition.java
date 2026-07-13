package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public record SlotItemAddition(int slot, boolean wasVacant, ItemStack addedItem, int addedAmount, int stackSizeOverride) {

  public SlotItemAddition(int slot, boolean wasVacant, ItemStack addedItem, int addedAmount) {
    this(slot, wasVacant, addedItem, addedAmount, 0);
  }

  public ItemStack makeStack() {
    var newItem = new ItemStack(addedItem);
    newItem.setAmount(addedAmount);
    return newItem;
  }

  public boolean performIfCanFit(Inventory inventory) {
    var currentItem = inventory.getItem(slot);

    if (!ItemUtil.isStackValid(currentItem)) {
      inventory.setItem(slot, makeStack());
      return true;
    }

    if (!currentItem.isSimilar(addedItem))
      return false;

    var stackSize = stackSizeOverride <= 0 ? currentItem.getMaxStackSize() : stackSizeOverride;
    var remainingSpace = stackSize - currentItem.getAmount();

    if (remainingSpace < addedAmount)
      return false;

    currentItem.setAmount(currentItem.getAmount() + addedAmount);
    return true;
  }

  public boolean isSimilarWithEqualParameters(SlotItemAddition other) {
    if (!other.addedItem.isSimilar(addedItem))
      return false;

    return slot == other.slot
      && wasVacant == other.wasVacant
      && addedAmount == other.addedAmount
      && stackSizeOverride == other.stackSizeOverride;
  }
}

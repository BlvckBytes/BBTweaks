package me.blvckbytes.bbtweaks.mechanic.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

  public static int addItemToInventoryAndGetRemainingAmount(ItemStack itemToAdd, int amount, Inventory inventory) {
    var firstVacantSlotIndex = -1;
    var remainingAmount = amount;

    // 1. Fill up all partial stacks

    for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        if (firstVacantSlotIndex < 0)
          firstVacantSlotIndex = slotIndex;

        continue;
      }

      if (!itemToAdd.isSimilar(currentItem))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      var amountToAdd = Math.min(remainingAmount, remainingSpace);

      currentItem.setAmount(currentItem.getAmount() + amountToAdd);

      remainingAmount -= amountToAdd;

      if (remainingAmount <= 0)
        return 0;
    }

    if (remainingAmount <= 0)
      return 0;

    // There's no need to once more scan for more vacant slots, as there aren't any left.
    if (firstVacantSlotIndex < 0)
      return remainingAmount;

    // 2. Since there's still some left to add, put as much as possible into the first vacant
    //    slot we've discovered while scanning for partial stacks.

    var remainder = new ItemStack(itemToAdd);

    var remainderAmount = Math.min(itemToAdd.getMaxStackSize(), remainingAmount);
    remainder.setAmount(remainderAmount);

    inventory.setItem(firstVacantSlotIndex, remainder);
    remainingAmount -= remainderAmount;

    if (remainingAmount <= 0)
      return 0;

    // 3. If there's still more, we need to fill up more than one vacant slot. Seeing how that's
    //    a rather seldom, special case, we don't keep a list of vacant slots but rather just
    //    iterate again - that's plenty fast.

    for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        remainder = new ItemStack(itemToAdd);

        remainderAmount = Math.min(itemToAdd.getMaxStackSize(), remainingAmount);
        remainder.setAmount(remainderAmount);

        inventory.setItem(slotIndex, remainder);
        remainingAmount -= remainderAmount;

        if (remainingAmount <= 0)
          return 0;
      }
    }

    return remainingAmount;
  }
}

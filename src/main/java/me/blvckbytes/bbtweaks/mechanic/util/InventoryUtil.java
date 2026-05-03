package me.blvckbytes.bbtweaks.mechanic.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

  public static int addItemToInventoryAndGetRemainingAmount(ItemStack itemToAdd, Inventory inventory) {
    var maxStackSize = itemToAdd.getMaxStackSize();

    var firstVacantSlotIndex = -1;
    var remainingAmount = itemToAdd.getAmount();

    for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        if (firstVacantSlotIndex < 0)
          firstVacantSlotIndex = slotIndex;

        continue;
      }

      if (!itemToAdd.isSimilar(currentItem))
        continue;

      var remainingSpace = maxStackSize - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      var amountToAdd = Math.min(remainingAmount, remainingSpace);

      currentItem.setAmount(currentItem.getAmount() + amountToAdd);

      remainingAmount -= amountToAdd;

      if (remainingAmount <= 0)
        break;
    }

    if (remainingAmount > 0 && firstVacantSlotIndex >= 0) {
      var remainder = new ItemStack(itemToAdd);
      remainder.setAmount(remainingAmount);
      inventory.setItem(firstVacantSlotIndex, remainder);
      remainingAmount = 0;
    }

    return remainingAmount;
  }
}

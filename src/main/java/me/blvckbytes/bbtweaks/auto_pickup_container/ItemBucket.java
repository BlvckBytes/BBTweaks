package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ItemBucket {

  public static final int INVENTORY_SIZE = 9 * 4;

  public final ItemStack item;

  private final int[] pickedUpCountBySlotIndex;
  private int totalCount;

  ItemBucket(ItemStack item) {
    this.item = item;
    this.pickedUpCountBySlotIndex = new int[INVENTORY_SIZE];
  }

  public int getTotalCount() {
    return totalCount;
  }

  public int getPickedUpCountForSlot(int slotIndex) {
    return pickedUpCountBySlotIndex[slotIndex];
  }

  public void analyzePickupSlots(ItemStack pickedUpItem, PlayerInventory playerInventory) {
    var firstVacantSlotIndex = -1;
    var remainingAmount = pickedUpItem.getAmount();

    for (var slotIndex = 0; slotIndex < INVENTORY_SIZE; ++slotIndex) {
      var currentItem = playerInventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        if (firstVacantSlotIndex < 0)
          firstVacantSlotIndex = slotIndex;

        continue;
      }

      if (!currentItem.isSimilar(pickedUpItem))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      var addedAmount = Math.min(remainingAmount, remainingSpace);

      addCountToSlot(slotIndex, addedAmount);

      remainingAmount -= addedAmount;

      if (remainingAmount <= 0)
        return;
    }

    if (remainingAmount > 0 && firstVacantSlotIndex >= 0)
      addCountToSlot(firstVacantSlotIndex, remainingAmount);
  }

  private void addCountToSlot(int slotIndex, int pickedUpCount) {
    pickedUpCountBySlotIndex[slotIndex] += pickedUpCount;
    totalCount += pickedUpCount;
  }
}


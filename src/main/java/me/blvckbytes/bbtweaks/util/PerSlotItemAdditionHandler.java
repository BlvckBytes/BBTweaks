package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;

public interface PerSlotItemAdditionHandler {

  /**
   * @param addedItem If the slot was vacant, the item is a clone of the item to add with the exact
   *                  amount set to that slot; otherwise, it's the item to add by reference.
   */
  void onAdditionPerSlot(int slot, boolean wasVacant, ItemStack addedItem, int addedAmount, int stackSizeOverride);

}

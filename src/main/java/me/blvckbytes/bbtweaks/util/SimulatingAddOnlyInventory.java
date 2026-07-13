package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SimulatingAddOnlyInventory extends AddOnlyInventory {

  public SimulatingAddOnlyInventory(
    Inventory capturedInventory,
    @Nullable PerSlotItemAdditionHandler perSlotItemAdditionHandler,
    @Nullable PerCallItemAdditionHandler perCallItemAdditionHandler
  ) {
    super(
      copyContents(capturedInventory.getStorageContents()),
      perSlotItemAdditionHandler,
      perCallItemAdditionHandler
    );
  }

  // Yes - not quite "add-only", but we only use this for simulating and some usage-sites need to reduce
  // as well. It's not at all critical, so instead of deriving again, I'm fine with this addition.
  public void setSlotAmount(int slot, int amount) {
    var targetItem = inventoryContents[slot];

    if (!ItemUtil.isStackValid(targetItem))
      return;

    if (amount <= 0) {
      targetItem.setAmount(0);
      inventoryContents[slot] = null;
      return;
    }

    targetItem.setAmount(amount);
  }

  @Override
  public boolean isSlotDisabled(int slot) {
    return false;
  }

  private static ItemStack[] copyContents(ItemStack[] contents) {
    var result = new ItemStack[contents.length];

    for (var index = 0; index < contents.length; ++index) {
      var currentItem = contents[index];

      if (!ItemUtil.isStackValid(currentItem))
        continue;

      result[index] = new ItemStack(currentItem);
    }

    return result;
  }
}

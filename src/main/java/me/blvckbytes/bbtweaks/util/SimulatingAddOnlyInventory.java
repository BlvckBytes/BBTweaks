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
      copyContents(capturedInventory.getContents()),
      perSlotItemAdditionHandler,
      perCallItemAdditionHandler
    );
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

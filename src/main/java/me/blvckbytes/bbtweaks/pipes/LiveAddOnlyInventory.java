package me.blvckbytes.bbtweaks.pipes;

import me.blvckbytes.bbtweaks.util.AddOnlyInventory;
import org.bukkit.block.Crafter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LiveAddOnlyInventory extends AddOnlyInventory {

  private final InventoryHolder inventoryHolder;

  private LiveAddOnlyInventory(Inventory inventory, InventoryHolder inventoryHolder) {
    super(
      inventory.getStorageContents(),
      // Account for the fact that mutating the contents-array does not set vacant slots in the inventory.
      (slot, wasVacant, addedItem, _, _) -> {
        if (wasVacant)
          inventory.setItem(slot, addedItem);
      },
      null
    );

    this.inventoryHolder = inventoryHolder;
  }

  public static LiveAddOnlyInventory fromInventoryHolder(InventoryHolder inventoryHolder) {
    return new LiveAddOnlyInventory(inventoryHolder.getInventory(), inventoryHolder);
  }

  @Override
  public boolean isSlotDisabled(int slot) {
    if (!(inventoryHolder instanceof Crafter crafter))
      return false;

    return crafter.isSlotDisabled(slot);
  }
}

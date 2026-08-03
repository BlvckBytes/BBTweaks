package me.blvckbytes.bbtweaks.pipes;

import me.blvckbytes.bbtweaks.util.AddOnlyInventory;
import org.bukkit.block.Crafter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

public class LiveAddOnlyInventory extends AddOnlyInventory {

  private final InventoryHolder inventoryHolder;

  public LiveAddOnlyInventory(Inventory inventory, @Nullable InventoryHolder inventoryHolder) {
    super(
      inventory.getSize(),
      inventory::getItem,
      inventory::setItem,
      null,
      null
    );

    this.inventoryHolder = inventoryHolder;
  }

  @Override
  public boolean isSlotDisabled(int slot) {
    if (!(inventoryHolder instanceof Crafter crafter))
      return false;

    return crafter.isSlotDisabled(slot);
  }
}

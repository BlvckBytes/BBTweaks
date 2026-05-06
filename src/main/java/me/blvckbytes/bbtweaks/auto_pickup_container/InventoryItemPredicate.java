package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface InventoryItemPredicate {

  boolean test(Inventory inventory, int slot, ItemStack item);

}

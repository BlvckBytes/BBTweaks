package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ShulkerPredicate {

  @Nullable DisableReason test(Inventory inventory, int slot, ItemStack item);

}

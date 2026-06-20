package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;

@FunctionalInterface
public interface ShulkerPredicate {

  EnumSet<DisableReason> test(Inventory inventory, int slot, ItemStack item);

}

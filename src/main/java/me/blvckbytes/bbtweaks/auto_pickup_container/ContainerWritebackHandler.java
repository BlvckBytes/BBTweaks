package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@FunctionalInterface
public interface ContainerWritebackHandler {

  void handle(ItemStack itemStack, ItemMeta meta, Inventory inventory);

}

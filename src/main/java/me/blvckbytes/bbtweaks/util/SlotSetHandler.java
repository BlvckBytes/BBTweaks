package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SlotSetHandler {

  void setItem(int slot, @Nullable ItemStack item);

}

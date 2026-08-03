package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SlotGetHandler {

  @Nullable ItemStack getItem(int slot);

}

package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;

public interface PerCallItemAdditionHandler {

  void onAdditionPerAddCall(ItemStack addedItem, int addedAmount, int stackSizeOverride);

}

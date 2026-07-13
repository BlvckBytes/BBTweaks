package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;

public final class ItemUtil {

  public static boolean isStackValid(ItemStack item) {
    if (item == null)
      return false;

    if (item.getType().isAir())
      return false;

    return item.getAmount() > 0;
  }
}

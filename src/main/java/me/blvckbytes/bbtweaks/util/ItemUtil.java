package me.blvckbytes.bbtweaks.util;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemUtil {

  public static boolean isStackValid(@Nullable ItemStack item) {
    if (item == null)
      return false;

    if (item.getType().isAir())
      return false;

    return item.getAmount() > 0;
  }
}

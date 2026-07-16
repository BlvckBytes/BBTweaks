package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public interface CachedRecipe {

  ItemStack getResultCopy();

  NamespacedKey getKey();

}

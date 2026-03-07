package me.blvckbytes.bbtweaks.mechanic.hopper;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface SlotPredicate {

  boolean test(int slotIndex, @Nullable ItemStack slotContents);

}

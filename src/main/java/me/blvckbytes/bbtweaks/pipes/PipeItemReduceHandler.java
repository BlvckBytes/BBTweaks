package me.blvckbytes.bbtweaks.pipes;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PipeItemReduceHandler {

  int handleAndGetRemainingAmount(int originSlot, @NotNull ItemStack item);

}

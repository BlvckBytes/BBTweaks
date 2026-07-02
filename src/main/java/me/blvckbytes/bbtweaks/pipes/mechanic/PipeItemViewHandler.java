package me.blvckbytes.bbtweaks.pipes.mechanic;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PipeItemViewHandler {

  void handle(int originSlot, @NotNull ItemStack item);

}

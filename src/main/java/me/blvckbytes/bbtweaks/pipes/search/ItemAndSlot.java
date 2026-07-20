package me.blvckbytes.bbtweaks.pipes.search;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public record ItemAndSlot(ItemStack item, Block block, Material type, int stackSize, int slot) {

  public ItemAndSlot(ItemStack item, Block block, int slot) {
    this(item, block, item.getType(), item.getMaxStackSize(), slot);
  }
}

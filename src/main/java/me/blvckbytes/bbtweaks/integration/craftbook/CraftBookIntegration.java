package me.blvckbytes.bbtweaks.integration.craftbook;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface CraftBookIntegration {

  List<ItemStack> requestPipeAndGetLeftovers(Block inputPistonBlock, List<ItemStack> items);

}

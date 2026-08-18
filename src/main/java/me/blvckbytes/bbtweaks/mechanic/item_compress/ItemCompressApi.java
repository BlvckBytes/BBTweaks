package me.blvckbytes.bbtweaks.mechanic.item_compress;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemCompressApi {

  void compressItemsInInventory(Inventory inventory);

  boolean isCompressedStack(ItemStack item);

  boolean isCompressedStackSimilarTo(ItemStack compressedStack, ItemStack other);

  int addAmountToCompressedStackAndGetRemaining(ItemStack compressedStack, int amountToAdd);

  @Nullable Integer getCompressedStackTotalAmount(ItemStack compressedStack);

  ItemStack createCompressedStackFrom(ItemStack item);

}

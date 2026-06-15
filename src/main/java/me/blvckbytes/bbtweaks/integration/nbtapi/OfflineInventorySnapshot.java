package me.blvckbytes.bbtweaks.integration.nbtapi;

import org.bukkit.inventory.ItemStack;

public record OfflineInventorySnapshot(
  ItemStack[] inventoryContents,

  ItemStack head,
  ItemStack chest,
  ItemStack legs,
  ItemStack feet,
  ItemStack offHand,

  ItemStack[] enderChestContents
) {}
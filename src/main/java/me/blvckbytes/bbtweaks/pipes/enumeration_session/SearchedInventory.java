package me.blvckbytes.bbtweaks.pipes.enumeration_session;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

public record SearchedInventory(
  Inventory inventory,
  Block block,
  @Nullable Block otherChestBlock,
  Material material,
  // Double-chests are represented by two individual containers, with each side
  // having its own snapshot-inventory. When accessing the double-chest in a live
  // manner, we'll get a view called a DoubleChestInventory - thus, the slots of the
  // other half need to be offset by the number if slots in a single chest.
  int slotOffset,
  @Nullable ItemPredicate pistonPredicate
) {}

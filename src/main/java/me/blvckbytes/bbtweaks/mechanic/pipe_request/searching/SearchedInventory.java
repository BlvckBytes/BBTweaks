package me.blvckbytes.bbtweaks.mechanic.pipe_request.searching;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

public class SearchedInventory {

  public final Inventory inventory;
  public final Block block;
  // Double-chests are represented by two individual containers, with each side
  // having its own snapshot-inventory. When accessing the double-chest in a live
  // manner, we'll get a view called a DoubleChestInventory - thus, the slots of the
  // other half need to be offset by the number if slots in a single chest.
  public final int slotOffset;

  public SearchedInventory(Inventory inventory, Block block, int slotOffset) {
    this.inventory = inventory;
    this.block = block;
    this.slotOffset = slotOffset;
  }
}

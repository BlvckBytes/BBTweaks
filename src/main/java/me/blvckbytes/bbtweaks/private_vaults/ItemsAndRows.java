package me.blvckbytes.bbtweaks.private_vaults;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class ItemsAndRows {

  private static final int FIXED_ITEMS_ARRAY_SIZE = 6 * 9;

  public final ItemStack[] items;
  public int lastKnownRows;

  private byte[] currentlyWrittenBytes;

  private ItemsAndRows(byte[] bytes) {
    if (bytes.length < 2)
      throw new IllegalArgumentException("Cannot load from less than two bytes");

    var itemsBytes = new byte[bytes.length - 1];
    System.arraycopy(bytes, 1, itemsBytes, 0, bytes.length - 1);

    var loadedItems = ItemStack.deserializeItemsFromBytes(itemsBytes);

    if (loadedItems.length != FIXED_ITEMS_ARRAY_SIZE) {
      var newArray = new ItemStack[FIXED_ITEMS_ARRAY_SIZE];
      System.arraycopy(loadedItems, 0, newArray, 0, Math.min(FIXED_ITEMS_ARRAY_SIZE, loadedItems.length));
      loadedItems = newArray;
    }

    this.items = loadedItems;
    this.lastKnownRows = bytes[0];
    this.currentlyWrittenBytes = bytes;
  }

  private ItemsAndRows(int lastKnownRows) {
    this.items = new ItemStack[FIXED_ITEMS_ARRAY_SIZE];
    this.lastKnownRows = lastKnownRows;
  }

  public static ItemsAndRows fromBytes(byte[] bytes) {
    return new ItemsAndRows(bytes);
  }

  public static ItemsAndRows empty(int lastKnownRows) {
    return new ItemsAndRows(lastKnownRows);
  }

  public void updateCurrentlyWrittenData(byte[] bytes) {
    currentlyWrittenBytes = bytes;
  }

  public boolean doBytesEqualCurrentlyWrittenData(byte[] bytes) {
    return Arrays.equals(currentlyWrittenBytes, bytes);
  }

  public byte[] toBytes() {
    var itemsBytes = ItemStack.serializeItemsAsBytes(items);
    var totalBytes = new byte[1 + itemsBytes.length];

    totalBytes[0] = (byte) lastKnownRows;
    System.arraycopy(itemsBytes, 0, totalBytes, 1, itemsBytes.length);

    return totalBytes;
  }

  public void loadIntoInventory(Inventory inventory) {
    for (var index = 0; index < inventory.getSize(); ++index) {
      if (index >= items.length)
        break;

      inventory.setItem(index, items[index]);
    }
  }

  public void updateFromInventory(Inventory inventory) {
    for (var index = 0; index < inventory.getSize(); ++index) {
      if (index >= items.length)
        break;

      items[index] = inventory.getItem(index);
    }
  }

  public int handOutExcessItemsAndGetStackCount(Player receiver) {
    var stackCount = 0;

    for (var index = lastKnownRows * 9; index < items.length; ++index) {
      var currentItem = items[index];

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      items[index] = null;

      receiver.getInventory()
        .addItem(currentItem)
        .values().forEach(item -> receiver.getWorld().dropItem(receiver.getEyeLocation(), item));

      ++stackCount;
    }

    return stackCount;
  }
}

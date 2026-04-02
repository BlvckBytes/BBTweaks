package me.blvckbytes.bbtweaks.private_vaults;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.Objects;

public class ItemsAndRows {

  private static final int FIXED_ITEMS_ARRAY_SIZE = 6 * 9;

  public final ItemStack[] items;
  public int lastKnownRows;

  private @Nullable String currentlyWrittenYamlString;

  private ItemsAndRows(@NotNull String yamlString) {
    var yamlConfig = YamlConfiguration.loadConfiguration(new StringReader(yamlString));

    this.items = new ItemStack[FIXED_ITEMS_ARRAY_SIZE];

    for (var index = 0; index < items.length; ++index)
      this.items[index] = yamlConfig.getItemStack(makeSlotKey(index));

    this.lastKnownRows = yamlConfig.getInt("lastKnownRows", 0);
    this.currentlyWrittenYamlString = yamlString;
  }

  private ItemsAndRows(int lastKnownRows) {
    this.items = new ItemStack[FIXED_ITEMS_ARRAY_SIZE];
    this.lastKnownRows = lastKnownRows;
  }

  public static ItemsAndRows fromYamlString(String yamlString) {
    return new ItemsAndRows(yamlString);
  }

  public static ItemsAndRows empty(int lastKnownRows) {
    return new ItemsAndRows(lastKnownRows);
  }

  public void updateCurrentlyWrittenYamlString(String yamlString) {
    currentlyWrittenYamlString = yamlString;
  }

  public boolean doesEqualCurrentlyWrittenYamlString(String yamlString) {
    return Objects.equals(currentlyWrittenYamlString, yamlString);
  }

  public String toYamlString() {
    var yamlConfig = new YamlConfiguration();

    yamlConfig.set("lastKnownRows", lastKnownRows);

    for (var index = 0; index < items.length; ++index) {
      var currentItem = items[index];

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      yamlConfig.set(makeSlotKey(index), currentItem);
    }

    return yamlConfig.saveToString();
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

  private static String makeSlotKey(int slot) {
    return "slot-" + slot;
  }
}

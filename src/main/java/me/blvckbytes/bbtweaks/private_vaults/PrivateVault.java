package me.blvckbytes.bbtweaks.private_vaults;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

public class PrivateVault {

  public final OfflinePlayer owner;
  public final ItemsAndRows items;
  public final @Nullable Inventory inventory;

  private long lastAccessStamp;

  private PrivateVault(OfflinePlayer owner, ItemsAndRows items, @Nullable Inventory inventory) {
    this.owner = owner;
    this.items = items;
    this.inventory = inventory;

    touchLastAccessStamp();
  }

  public static PrivateVault loadFromItems(OfflinePlayer owner, ItemsAndRows items, Component inventoryTitle) {
    if (items.lastKnownRows <= 0)
      return new PrivateVault(owner, items, null);

    var inventory = Bukkit.createInventory(null, 9 * items.lastKnownRows, inventoryTitle);

    items.loadIntoInventory(inventory);

    return new PrivateVault(owner, items, inventory);
  }

  public ItemsAndRows syncAndGetItems() {
    if (inventory != null)
      items.updateFromInventory(inventory);

    return items;
  }

  public void touchLastAccessStamp() {
    lastAccessStamp = System.currentTimeMillis();
  }

  public long getLastAccessStamp() {
    return lastAccessStamp;
  }
}

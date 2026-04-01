package me.blvckbytes.bbtweaks.private_vaults;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PrivateVault {

  public final OfflinePlayer owner;

  private final ItemsAndRows items;
  private final Component inventoryTitle;
  private @Nullable Inventory inventory;

  private long lastAccessStamp;

  private PrivateVault(OfflinePlayer owner, ItemsAndRows items, Component inventoryTitle) {
    this.owner = owner;
    this.items = items;
    this.inventoryTitle = inventoryTitle;

    if (items.lastKnownRows > 0) {
      this.inventory = Bukkit.createInventory(null, 9 * items.lastKnownRows, inventoryTitle);
      items.loadIntoInventory(this.inventory);
    }

    touchLastAccessStamp();
  }

  public static PrivateVault loadFromItems(OfflinePlayer owner, ItemsAndRows items, Component inventoryTitle) {
    return new PrivateVault(owner, items, inventoryTitle);
  }

  public void updateNumberOfRows(int rows, ConfigKeeper<MainSection> config) {
    if (rows == items.lastKnownRows)
      return;

    items.lastKnownRows = rows;

    List<HumanEntity> viewers = null;

    if (inventory != null) {
      viewers = new ArrayList<>(inventory.getViewers());
      viewers.forEach(HumanEntity::closeInventory);

      items.updateFromInventory(inventory);
      inventory = null;
    }

    if (rows == 0) {
      if (viewers == null)
        return;

      viewers.forEach(viewer -> config.rootSection.privateVaults.vaultResizedToZero.sendMessage(viewer));
      return;
    }

    this.inventory = Bukkit.createInventory(null, 9 * items.lastKnownRows, inventoryTitle);

    items.loadIntoInventory(this.inventory);

    if (viewers == null)
      return;

    viewers.forEach(viewer -> {
      if (viewer instanceof Player player)
        openInventoryIfExistsAndHandOutExcesses(player, config);
    });
  }

  public VaultAccessResult openInventoryIfExistsAndHandOutExcesses(Player viewer, ConfigKeeper<MainSection> config) {
    if (viewer.getUniqueId().equals(owner.getUniqueId())) {
      var excessCount = items.handOutExcessItemsAndGetStackCount(viewer);

      if (excessCount > 0) {
        config.rootSection.privateVaults.handedOutExcessStacks.sendMessage(
          viewer,
          new InterpretationEnvironment()
            .withVariable("stack_count", excessCount)
        );
      }
    }

    if (inventory == null)
      return VaultAccessResult.OWNER_CANNOT_ACCESS_ANY_ROWS;

    viewer.openInventory(inventory);
    return VaultAccessResult.SUCCESS;
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

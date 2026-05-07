package me.blvckbytes.bbtweaks.shulker_accessor.change_detection;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class ChangeDetectionHolder implements InventoryHolder {

  private boolean dirty;
  private boolean locked;
  private int viewCount;

  private @Nullable Inventory inventory;

  public abstract boolean isValid();

  public abstract void onInventoryOpen(Player viewer);

  public abstract void onInventoryClose(Player viewer);

  public void markDirty() {
    dirty = true;
  }

  public boolean isDirty() {
    return dirty;
  }

  public boolean isLocked() {
    return locked;
  }

  public void clearDirty() {
    dirty = false;
  }

  public int getViewCount() {
    return viewCount;
  }

  public void incrementViewCount() {
    ++viewCount;
  }

  public void decrementViewCount() {
    --viewCount;
  }

  public void setInventory(@NotNull Inventory inventory) {
    if (this.inventory != null)
      throw new IllegalStateException("An inventory-reference was already set");

    this.inventory = inventory;
  }

  @Override
  public @NotNull Inventory getInventory() {
    if (inventory == null)
      throw new IllegalStateException("Expected the inventory-refence to have been set");

    return inventory;
  }

  public void closeAll() {
    new ArrayList<>(getInventory().getViewers()).forEach(HumanEntity::closeInventory);
  }

  public void markLockedAndCloseAllNextTick(Plugin plugin) {
    this.locked = true;

    Bukkit.getScheduler().runTaskLater(plugin, this::closeAll, 1L);
  }
}

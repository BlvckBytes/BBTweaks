package me.blvckbytes.bbtweaks.shulker_accessor.change_detection;

import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class ChangeDetectionHolder implements InventoryHolder {

  private static long instanceCounter;

  private final long identifier;
  private boolean dirty;
  private int viewCount;

  private @Nullable Inventory inventory;

  public ChangeDetectionHolder() {
    identifier = ++instanceCounter;
  }

  public abstract boolean isValid();

  public void markDirty() {
    dirty = true;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void clearDirty() {
    dirty = false;
  }

  public long getIdentifier() {
    return identifier;
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
}

package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetaRecipeInventoryHolder implements InventoryHolder {

  public @Nullable AutoCrafterInstance instance;

  @Override
  public @NotNull Inventory getInventory() {
    if (instance == null)
      throw new IllegalStateException();

    return instance.metaRecipeInventory;
  }
}

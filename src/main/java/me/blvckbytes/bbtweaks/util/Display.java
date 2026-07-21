package me.blvckbytes.bbtweaks.util;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class Display<DisplayDataType> implements InventoryHolder {

  public final Player player;
  protected final ConfigKeeper<MainSection> config;
  protected final Plugin plugin;
  public final DisplayDataType displayData;
  protected Inventory inventory;

  protected Display(
    Player player,
    DisplayDataType displayData,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.player = player;
    this.displayData = displayData;
    this.config = config;
    this.plugin = plugin;
  }

  public void show() {
    inventory = makeInventoryParameters().makeInventory(this);

    renderItems();

    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
  }

  protected abstract void renderItems();

  protected abstract DisplayInventoryParameters makeInventoryParameters();

  public abstract void onConfigReload();

  public boolean isInventory(Inventory inventory) {
    return Objects.equals(this.inventory, inventory);
  }

  @Override
  public @NotNull Inventory getInventory() {
    return inventory;
  }

  public int getSize() {
    return inventory == null ? 0 : inventory.getSize();
  }
}

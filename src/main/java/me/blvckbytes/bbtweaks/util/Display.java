package me.blvckbytes.bbtweaks.util;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class Display<DisplayDataType> implements InventoryHolder {

  public final Player player;
  protected final ConfigKeeper<MainSection> config;
  protected final Plugin plugin;
  public final DisplayDataType displayData;
  private Inventory inventory;

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

  public void showNextTick() {
    Bukkit.getScheduler().runTaskLater(plugin, this::show, 1);
  }

  public void show() {
    inventory = makeInventoryParameters().makeInventory(this);

    updateItems();

    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inventory));
  }

  protected abstract void renderItems(ItemConsumer itemConsumer);

  protected abstract @Nullable GuiItemStackSection getFillerItem();

  protected abstract DisplayInventoryParameters makeInventoryParameters();

  public void updateItems() {
    var renderedIntoSlotIndices = new IntArraySet();

    renderItems((slot, item) -> {
      renderedIntoSlotIndices.add(slot);
      setItemIfDifferent(slot, item);
    });

    var fillerItem = getFillerItem();

    if (fillerItem == null)
      return;

    ItemStack builtFillerItem = null;

    for (var fillerSlot : fillerItem.getDisplaySlots()) {
      if (renderedIntoSlotIndices.contains((int) fillerSlot))
        continue;

      if (builtFillerItem == null)
        builtFillerItem = fillerItem.build(null);

      setItemIfDifferent(fillerSlot, builtFillerItem);
    }
  }

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

  private void setItemIfDifferent(int slot, ItemStack newItem) {
    if (inventory == null || slot < 0 || slot >= inventory.getSize())
      return;

    var existingItem = inventory.getItem(slot);

    if (!Objects.equals(existingItem, newItem))
      inventory.setItem(slot, newItem);
  }
}

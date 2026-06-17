package me.blvckbytes.bbtweaks.shulker_accessor;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

public class ShulkerAccessorWriteEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final Material type;
  public final BlockStateMeta meta;
  public final Inventory inventory;

  public ShulkerAccessorWriteEvent(Material type, BlockStateMeta meta, Inventory inventory) {
    this.type = type;
    this.meta = meta;
    this.inventory = inventory;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }
}

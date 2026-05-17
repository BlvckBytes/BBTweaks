package me.blvckbytes.bbtweaks.shulker_accessor;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

public class ShulkerAccessorWriteEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final Material type;
  public final BlockStateMeta meta;

  public ShulkerAccessorWriteEvent(Material type, BlockStateMeta meta) {
    this.type = type;
    this.meta = meta;
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

package me.blvckbytes.bbtweaks.shulker_accessor.change_detection;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class InventoryInvalidatedEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  private final ChangeDetectionHolder holder;

  public InventoryInvalidatedEvent(ChangeDetectionHolder holder) {
    this.holder = holder;
  }

  public ChangeDetectionHolder getHolder() {
    return holder;
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

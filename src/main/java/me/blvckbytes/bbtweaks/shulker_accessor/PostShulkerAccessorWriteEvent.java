package me.blvckbytes.bbtweaks.shulker_accessor;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PostShulkerAccessorWriteEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final ItemStack item;

  public PostShulkerAccessorWriteEvent(ItemStack item) {
    this.item = item;
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

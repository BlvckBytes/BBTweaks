package me.blvckbytes.bbtweaks.inv_magnet;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PreAttractItemEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final ItemStack attractedItem;

  private boolean cancelled;
  private boolean canHoldSome;

  public PreAttractItemEvent(Player player, ItemStack attractedItem) {
    this.player = player;
    this.attractedItem = attractedItem;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public boolean canHoldSome() {
    return this.canHoldSome;
  }

  public void markCanHoldSome() {
    this.canHoldSome = true;
  }

  public Player getPlayer() {
    return player;
  }

  public ItemStack getAttractedItem() {
    return attractedItem;
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

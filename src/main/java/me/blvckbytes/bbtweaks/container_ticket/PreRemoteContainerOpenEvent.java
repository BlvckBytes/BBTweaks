package me.blvckbytes.bbtweaks.container_ticket;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class PreRemoteContainerOpenEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final Block containerBlock;
  public final Inventory containerInventory;

  public PreRemoteContainerOpenEvent(Block containerBlock, Inventory containerInventory) {
    this.containerBlock = containerBlock;
    this.containerInventory = containerInventory;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlers;
  }

  public static @NotNull HandlerList getHandlerList() {
    return handlers;
  }
}

package me.blvckbytes.bbtweaks.auto_tool;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class AutoToolExternalEnableEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  public final Player player;
  public final Block hitBlock;
  public final ItemStack heldItem;

  private boolean enable;

  public AutoToolExternalEnableEvent(
    Player player,
    Block hitBlock,
    ItemStack heldItem
  ) {
    this.player = player;
    this.hitBlock = hitBlock;
    this.heldItem = heldItem;
  }

  public boolean shouldEnable() {
    return enable;
  }

  public void setShouldEnable() {
    this.enable = true;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}

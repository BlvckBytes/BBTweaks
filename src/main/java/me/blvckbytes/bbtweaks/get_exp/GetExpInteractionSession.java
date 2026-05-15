package me.blvckbytes.bbtweaks.get_exp;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class GetExpInteractionSession {

  public final Player player;
  public final Consumer<Block> interactionHandler;
  public boolean allowMultiUse;
  private long lastUse;

  public GetExpInteractionSession(Player player, Consumer<Block> interactionHandler) {
    this.player = player;
    this.interactionHandler = interactionHandler;
    this.touchExpiry();
  }

  public void touchExpiry() {
    this.lastUse = System.currentTimeMillis();
  }

  public boolean isExpired(int expirySeconds) {
    return System.currentTimeMillis() - this.lastUse >= (long) expirySeconds * 1000L;
  }
}

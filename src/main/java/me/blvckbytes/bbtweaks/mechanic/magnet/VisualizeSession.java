package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.entity.Player;

public abstract class VisualizeSession {

  public final Player player;

  private final long createdAt;
  private final long maxLifetime;

  protected VisualizeSession(Player player, long maxLifetime) {
    this.player = player;
    this.createdAt = System.currentTimeMillis();
    this.maxLifetime = maxLifetime;
  }

  public boolean isExpired() {
    if (maxLifetime <= 0)
      return false;

    return System.currentTimeMillis() - createdAt > maxLifetime;
  }

  public abstract Cuboid getCuboid();

}

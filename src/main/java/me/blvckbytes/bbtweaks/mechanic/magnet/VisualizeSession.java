package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public abstract class VisualizeSession {

  public final Player player;
  public final Sign sign;

  private final long createdAt;
  private final long maxLifetime;

  private boolean manuallyExpired;

  protected VisualizeSession(Player player, Sign sign, long maxLifetime) {
    this.player = player;
    this.sign = sign;
    this.createdAt = System.currentTimeMillis();
    this.maxLifetime = maxLifetime;
  }

  protected void manuallyExpire() {
    manuallyExpired = true;
  }

  public boolean isExpired() {
    if (manuallyExpired)
      return true;

    if (maxLifetime <= 0)
      return false;

    return System.currentTimeMillis() - createdAt > maxLifetime;
  }

  public abstract Cuboid getCuboid();

}

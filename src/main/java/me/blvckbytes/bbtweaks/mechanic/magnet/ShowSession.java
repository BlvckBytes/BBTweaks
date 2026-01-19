package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public class ShowSession extends VisualizeSession {

  private final Cuboid cuboid;

  public ShowSession(Player player, Sign sign, Cuboid cuboid, long durationMs) {
    super(player, sign, durationMs);

    this.cuboid = cuboid;
  }

  @Override
  public Cuboid getCuboid() {
    return cuboid;
  }
}

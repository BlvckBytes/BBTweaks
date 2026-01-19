package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import org.bukkit.entity.Player;

public class ShowSession extends VisualizeSession {

  private final Cuboid cuboid;

  public ShowSession(Player player, Cuboid cuboid, ConfigKeeper<MainSection> config) {
    super(player, config.rootSection.mechanic.magnet.visualization.durationMs);

    this.cuboid = cuboid;
  }

  @Override
  public Cuboid getCuboid() {
    return cuboid;
  }
}

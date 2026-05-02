package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;

public class InvMagnetParameters {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  private int radius;
  public boolean enabled;

  private InvMagnetLimits limits;

  public InvMagnetParameters(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    updateLimitsAndConstrain();
  }

  public InvMagnetLimits getLimits() {
    return limits;
  }

  public int getRadius() {
    return radius;
  }

  public boolean setRadiusAndGetIfExceeded(int radius) {
    var exceeded = false;

    if (radius < 0)
      radius = 0;

    if (radius > limits.maxRadius()) {
      radius = limits.maxRadius();
      exceeded = true;
    }

    this.radius = radius;

    return exceeded;
  }

  public void updateLimitsAndConstrain() {
    this.limits = getApplyingLimits();
    this.radius = Math.min(this.radius, limits.maxRadius());
  }

  private InvMagnetLimits getApplyingLimits() {
    for (var limits : config.rootSection.invMagnet.limitsInDescendingOrder) {
      if (!player.hasPermission("bbtweaks.invmagnet.tier." + limits.tierName()))
        continue;

      return limits;
    }

    return InvMagnetLimits.ZERO;
  }
}

package me.blvckbytes.bbtweaks.durability_warnings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class SlotTrackingInfo {

  // TODO: Properly implement threshold after fixing the regression

  private final ConfigKeeper<MainSection> config;
  private final IntSet playedNotificationPercentages;

  private @Nullable Material material;
  private int lastKnownDamage;

  public SlotTrackingInfo(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.playedNotificationPercentages = new IntOpenHashSet();
  }

  public void updateAndPossiblyReset(Material material, int damage) {
    if (this.material != null) {
      var damageDelta = this.lastKnownDamage - damage;

      // Different tool or has been repaired
      if (this.material != material || damageDelta > 0)
        reset();
    }

    this.material = material;
    this.lastKnownDamage = damage;
  }

  public void reset() {
    material = null;
    playedNotificationPercentages.clear();
  }

  public boolean shouldPlayNotification(int notificationPercentage) {
    return playedNotificationPercentages.add(notificationPercentage);
  }
}

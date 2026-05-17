package me.blvckbytes.bbtweaks.durability_warnings;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class SlotTrackingInfo {

  private final IntSet playedNotificationPercentages = new IntOpenHashSet();

  private @Nullable Material material;
  private int lastKnownDamage;

  public void updateAndPossiblyReset(Material material, int damage) {
    if (this.material != null) {
      // Different tool or has been repaired
      if (this.material != material || this.lastKnownDamage > damage)
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

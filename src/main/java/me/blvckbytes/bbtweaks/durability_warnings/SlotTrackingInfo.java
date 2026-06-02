package me.blvckbytes.bbtweaks.durability_warnings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class SlotTrackingInfo {

  private final ConfigKeeper<MainSection> config;
  private final IntSet playedNotificationPercentages;

  private @Nullable Material material;
  private int lastKnownDamage;

  private final IntRingbuffer lastDamageDeltas;

  public SlotTrackingInfo(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.playedNotificationPercentages = new IntOpenHashSet();
    this.lastDamageDeltas = new IntRingbuffer(512);
  }

  public void updateAndPossiblyReset(Material material, int damage) {
    if (this.material != null) {
      // > 0 if repaired and < 0 if further worn down.
      var damageDelta = this.lastKnownDamage - damage;

      lastDamageDeltas.add(damageDelta);

      // Different tool or has been repaired far enough
      if (this.material != material || lastDamageDeltas.calculateSum() >= config.rootSection.durabilityWarnings.trackingResetMinDurabilityDelta)
        reset();
    }

    this.material = material;
    this.lastKnownDamage = damage;
  }

  public void reset() {
    material = null;
    playedNotificationPercentages.clear();
    lastDamageDeltas.clear();
  }

  public boolean shouldPlayNotification(int notificationPercentage) {
    return playedNotificationPercentages.add(notificationPercentage);
  }
}

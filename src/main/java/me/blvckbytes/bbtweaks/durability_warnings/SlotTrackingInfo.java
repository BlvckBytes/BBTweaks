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

      var deltaSum = lastDamageDeltas.calculateSum();

      // What we're trying to measure is a positive delta-sum "streak" that exceeds the configured
      // threshold as to then unmark notification-percentages as used. If the sum turned negative,
      // the player has damaged the tool more than they've repaired it, so they are definitely merely
      // picking up a few exp-orbs here and there, which is exactly what we're trying to avoid causing
      // a reset. Proper repairing will cause a far longer streak. This way, we're also not carrying
      // a huge negative sum that has then to be exceeded (plus the positive delta!) when repairing.
      if (deltaSum < 0)
        lastDamageDeltas.clear();

      // Different tool or has been repaired far enough
      if (this.material != material || deltaSum >= config.rootSection.durabilityWarnings.trackingResetMinDurabilityDelta)
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

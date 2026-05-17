package me.blvckbytes.bbtweaks.durability_warnings;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.durability_warnings.config.DurabilityWarningSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.List;

public class DurabilityTrackingSession {

  private final WarningsProfile profile;

  private final SlotTrackingInfo[] trackingInfoBySlot;

  public DurabilityTrackingSession(WarningsProfile profile) {
    this.profile = profile;

    // Hotbar and off-hand
    this.trackingInfoBySlot = new SlotTrackingInfo[9 + 1];
  }

  public void submitDamageUpdate(int slot, ItemStack item, int damage, List<DurabilityWarningSection> applicativeWarnings) {
    if (slot == PlayerHand.OFFHAND_SLOT_INDEX)
      slot = 9;

    if (slot < 0 || slot >= trackingInfoBySlot.length)
      return;

    var trackingInfo = trackingInfoBySlot[slot];

    if (trackingInfo == null) {
      trackingInfo = new SlotTrackingInfo();
      trackingInfoBySlot[slot] = trackingInfo;
    }

    var maxDurability = item.getType().getMaxDurability();

    if (maxDurability <= 0) {
      trackingInfo.reset();
      return;
    }

    var type = item.getType();

    trackingInfo.updateAndPossiblyReset(type, damage);

    var remainingDurabilityPercentage = Math.max(1, (int) Math.floor((1 - (damage / (double) maxDurability)) * 100));

    var environment = new InterpretationEnvironment()
      .withVariable("damage", damage)
      .withVariable("max_durability", maxDurability)
      .withVariable("remaining_durability_percentage", remainingDurabilityPercentage)
      .withVariable("type_key", type.translationKey())
      .withVariable("slot", slot + 1)
      .withVariable("is_offhand", slot > 8);

    for (var applicativeWarning : applicativeWarnings) {
      var notification = applicativeWarning.getNotificationAtPercentage(remainingDurabilityPercentage);

      if (notification == null)
        continue;

      if (!trackingInfo.shouldPlayNotification(notification.percentage))
        continue;

      notification.displayTo(profile.player, profile.playSound, environment);
      return;
    }
  }

  public void submitSlotUpdate(int slot, ItemStack newItem) {
    if (slot == PlayerHand.OFFHAND_SLOT_INDEX)
      slot = 9;

    if (slot < 0 || slot >= trackingInfoBySlot.length)
      return;

    var trackingInfo = trackingInfoBySlot[slot];

    if (trackingInfo == null)
      return;

    if (!(newItem.getItemMeta() instanceof Damageable damageable)) {
      trackingInfo.reset();
      return;
    }

    trackingInfo.updateAndPossiblyReset(newItem.getType(), damageable.getDamage());
  }
}

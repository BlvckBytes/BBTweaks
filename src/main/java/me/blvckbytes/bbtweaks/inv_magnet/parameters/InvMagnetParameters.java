package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.settings.ItemAttemptsKeeper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class InvMagnetParameters extends ItemAttemptsKeeper {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public final Object2BooleanMap<String> enabledByWorldGroupIdentifyingName;
  public final Object2IntMap<String> radiusByWorldGroupIdentifyingName;

  private InvMagnetLimits limits;

  public InvMagnetParameters(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    this.enabledByWorldGroupIdentifyingName = new Object2BooleanOpenHashMap<>();
    this.enabledByWorldGroupIdentifyingName.defaultReturnValue(false);

    this.radiusByWorldGroupIdentifyingName = new Object2IntOpenHashMap<>();
    this.radiusByWorldGroupIdentifyingName.defaultReturnValue(0);

    updateLimitsAndConstrain();
  }

  public InvMagnetLimits getLimits() {
    return limits;
  }

  public void setEnabledAndMessage(Player player, @Nullable Boolean value) {
    ensureNonZeroLimits();

    var currentValue = enabledByWorldGroupIdentifyingName.getBoolean(limits.worldGroupIdentifyingName());
    var newValue = value == null ? !currentValue : value;

    if (newValue == currentValue) {
      if (newValue) {
        config.rootSection.invMagnet.alreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.invMagnet.alreadyDisabled.sendMessage(player);
      return;
    }

    enabledByWorldGroupIdentifyingName.put(limits.worldGroupIdentifyingName(), newValue);

    if (newValue) {
      config.rootSection.invMagnet.nowEnabled.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("radius", getRadius())
          .withVariable("world_group_name", limits.worldGroupDisplayName())
      );
      return;
    }

    config.rootSection.invMagnet.nowDisabled.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("radius", getRadius())
        .withVariable("world_group_name", limits.worldGroupDisplayName())
    );
  }

  public boolean isEnabled() {
    if (limits.isZero())
      return false;

    return enabledByWorldGroupIdentifyingName.getBoolean(limits.worldGroupIdentifyingName());
  }

  public int getRadius() {
    if (limits.isZero())
      return 0;

    return radiusByWorldGroupIdentifyingName.getInt(limits.worldGroupIdentifyingName());
  }

  public boolean setRadiusAndGetIfExceeded(int radius) {
    ensureNonZeroLimits();

    var exceeded = false;

    if (radius < 0)
      radius = 0;

    if (radius > limits.maxRadius()) {
      radius = limits.maxRadius();
      exceeded = true;
    }

    radiusByWorldGroupIdentifyingName.put(limits.worldGroupIdentifyingName(), radius);

    return exceeded;
  }

  public @Nullable InvMagnetLimits updateLimitsAndConstrain() {
    this.limits = getApplyingLimits();

    if (!this.limits.isZero()) {
      setRadiusAndGetIfExceeded(getRadius());
      return limits;
    }

    return null;
  }

  private InvMagnetLimits getApplyingLimits() {
    var worldNameLower = player.getWorld().getName().toLowerCase();

    for (var worldGroup : config.rootSection.invMagnet.worldGroups) {
      if (!worldGroup.worlds.contains(worldNameLower))
        continue;

      if (!player.hasPermission("bbtweaks.invmagnet.world-group." + worldGroup.identifyingName))
        continue;

      for (var limits : worldGroup.limitsInDescendingOrder) {
        if (!player.hasPermission("bbtweaks.invmagnet.tier." + limits.tierName()))
          continue;

        return limits;
      }
    }

    return InvMagnetLimits.ZERO;
  }

  private void ensureNonZeroLimits() {
    if (limits.isZero())
      throw new IllegalStateException("Cannot set radius if no limits are available; check before calling");
  }
}

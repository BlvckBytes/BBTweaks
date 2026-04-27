package me.blvckbytes.bbtweaks.multi_break.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.config.MultiBreakLimits;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MultiBreakParameters {

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  public @Nullable PredicateAndLanguage filter;
  public SneakMode sneakMode;

  public final int[] extentByOrdinal;

  private MultiBreakLimits limits;

  private boolean exceededVolumeLimit;
  private boolean exceededExtentLimit;

  public MultiBreakParameters(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    this.sneakMode = SneakMode.NONE;
    this.extentByOrdinal = new int[BreakExtent.values.size()];

    updateLimits();
  }

  public MultiBreakLimits getLimits() {
    return limits;
  }

  public void updateLimits() {
    for (var limits : config.rootSection.multiBreak.limitsInAscendingOrder) {
      if (!player.hasPermission("bbtweaks.multibreak.tier." + limits.tierName()))
        continue;

      this.limits = limits;
      return;
    }

    this.limits = MultiBreakLimits.ZERO;
  }

  public void zeroOutAllExtents() {
    Arrays.fill(extentByOrdinal, 0);
  }

  public void constrain(boolean setFlags) {
    constrainAndSetFlags(null, setFlags);
  }

  private int calculateVolume() {
    return (getExtent(BreakExtent.LEFT) + 1 + getExtent(BreakExtent.RIGHT))
      * (getExtent(BreakExtent.UP) + 1 + getExtent(BreakExtent.DOWN))
      * (1 + getExtent(BreakExtent.DEPTH));
  }

  private void constrainAndSetFlags(@Nullable BreakExtent manipulatedExtent, boolean setFlags) {
    while (limits.maxVolume() > 0 && calculateVolume() > limits.maxVolume()) {
      var extentToDecrement = getNonZeroManipulatedOrFindGreatestExtent(manipulatedExtent);

      if (extentToDecrement == null)
        break;

      --extentByOrdinal[extentToDecrement.ordinal()];

      if (setFlags)
        exceededVolumeLimit = true;
    }

    if (limits.maxExtent() > 0) {
      for (var currentExtent : BreakExtent.values) {
        var currentValue = extentByOrdinal[currentExtent.ordinal()];

        if (currentValue <= limits.maxExtent())
          continue;

        extentByOrdinal[currentExtent.ordinal()] = limits.maxExtent();

        if (setFlags)
          exceededExtentLimit = true;
      }
    }
  }

  private @Nullable BreakExtent getNonZeroManipulatedOrFindGreatestExtent(@Nullable BreakExtent manipulatedExtent) {
    if (manipulatedExtent != null && extentByOrdinal[manipulatedExtent.ordinal()] > 0)
      return manipulatedExtent;

    BreakExtent maxExtent = null;
    int maxValue = 0;

    for (var currentExtent : BreakExtent.values) {
      var currentValue = extentByOrdinal[currentExtent.ordinal()];

      if (currentValue <= 0)
        continue;

      if (maxExtent == null || currentValue > maxValue) {
        maxExtent = currentExtent;
        maxValue = currentValue;
      }
    }

    return maxExtent;
  }

  public boolean didExceedVolumeLimit() {
    return exceededVolumeLimit;
  }

  public boolean didExceedExtentLimit() {
    return exceededExtentLimit;
  }

  public void clearFlags() {
    exceededVolumeLimit = false;
    exceededExtentLimit = false;
  }

  public int getExtent(BreakExtent extent) {
    return extentByOrdinal[extent.ordinal()];
  }

  public void setExtent(BreakExtent extent, int value, boolean constrain) {
    if (value < 0)
      value = 0;

    extentByOrdinal[extent.ordinal()] = value;

    if (!constrain)
      return;

    clearFlags();
    constrainAndSetFlags(extent, true);
  }

  public void increaseExtent(BreakExtent extent) {
    setExtent(extent, getExtent(extent) + 1, true);
  }

  public void decreaseExtent(BreakExtent extent) {
    setExtent(extent, getExtent(extent) - 1, true);
  }
}
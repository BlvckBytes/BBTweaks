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
  private final boolean[] didExceedLimitByDimensionOrdinal;

  private MultiBreakLimits limits;

  public MultiBreakParameters(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    this.sneakMode = SneakMode.NONE;
    this.extentByOrdinal = new int[BreakExtent.values.size()];
    this.didExceedLimitByDimensionOrdinal = new boolean[BreakDimension.values.size()];

    updateLimits();
  }

  public MultiBreakLimits getLimits() {
    return limits;
  }

  public void updateLimits() {
    for (var limits : config.rootSection.multiBreak.limitsInDescendingOrder) {
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

  public void constrainAndSetFlags(boolean setFlags) {
    constrainAndSetFlags(null, setFlags);
  }

  private void constrainAndSetFlags(@Nullable BreakExtent manipulatedExtent, boolean setFlags) {
    for (var dimension : BreakDimension.values) {
      while (true) {
        // The sum always starts out at one, seeing how the origin-block is included in the total outer cuboid-dimension.
        var sum = 1;

        BreakExtent largestOrTargetExtent = null;
        var largestExtentValue = 0;

        for (var currentExtent : dimension.extents) {
          var currentValue = extentByOrdinal[currentExtent.ordinal()];

          // Lock in to the manipulated extent, as to decrease it, instead of another member
          // of the current dimension, as to keep the value appearing constant when the
          // player tries to increment it within the UI.
          if (currentValue > 0 && currentExtent == manipulatedExtent) {
            largestOrTargetExtent = currentExtent;
            largestExtentValue = -1;
          }

          if (largestOrTargetExtent == null || (largestExtentValue >= 0 && currentValue > largestExtentValue)) {
            largestOrTargetExtent = currentExtent;
            largestExtentValue = currentValue;
          }

          sum += currentValue;
        }

        if (sum <= limits.maxDimension() || largestOrTargetExtent == null)
          break;

        --extentByOrdinal[largestOrTargetExtent.ordinal()];

        if (setFlags)
          didExceedLimitByDimensionOrdinal[dimension.ordinal()] = true;
      }
    }
  }

  public boolean didExceedLimit(BreakDimension dimension) {
    return didExceedLimitByDimensionOrdinal[dimension.ordinal()];
  }

  public void clearFlags() {
    Arrays.fill(didExceedLimitByDimensionOrdinal, false);
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
package me.blvckbytes.bbtweaks.multi_break.parameters;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.multi_break.command.CommandAction;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MultiBreakParameters {

  private final MultiBreakParametersSlots parametersSlots;
  public final int slotIndex;

  public @Nullable PredicateAndLanguage filter;
  public boolean filterEnabled;
  public SneakMode sneakMode;
  public boolean locked;

  public final int[] extentByOrdinal;
  private final boolean[] didExceedLimitByDimensionOrdinal;

  public MultiBreakParameters(MultiBreakParametersSlots parametersSlots, int slotIndex) {
    this.parametersSlots = parametersSlots;
    this.slotIndex = slotIndex;
    this.sneakMode = SneakMode.NONE;
    this.extentByOrdinal = new int[BreakExtent.values.size()];
    this.didExceedLimitByDimensionOrdinal = new boolean[BreakDimension.values.size()];
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

        if (sum <= parametersSlots.getLimits().maxDimension() || largestOrTargetExtent == null)
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

  public int getDimension(BreakDimension dimension) {
    // Always starting out with one, as that's the origin-block itself
    return Arrays.stream(dimension.extents).map(this::getExtent).reduce(1, Integer::sum);
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

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("total_width", getDimension(BreakDimension.WIDTH))
      .withVariable("total_height", getDimension(BreakDimension.HEIGHT))
      .withVariable("total_depth", getDimension(BreakDimension.DEPTH))
      .withVariable("extent_left", getExtent(BreakExtent.LEFT))
      .withVariable("extent_right", getExtent(BreakExtent.RIGHT))
      .withVariable("extent_up", getExtent(BreakExtent.UP))
      .withVariable("extent_down", getExtent(BreakExtent.DOWN))
      .withVariable("extent_depth", getExtent(BreakExtent.DEPTH))
      .withVariable("max_dimension", parametersSlots.getLimits().maxDimension())
      .withVariable("enabled", parametersSlots.enabled)
      .withVariable("sneak_mode", sneakMode.name())
      .withVariable("filter_predicate", filter == null ? null : filter.getTokenPredicateString())
      .withVariable("filter_enabled", filterEnabled)
      .withVariable("exceeded_width_limit", didExceedLimit(BreakDimension.WIDTH))
      .withVariable("exceeded_height_limit", didExceedLimit(BreakDimension.HEIGHT))
      .withVariable("exceeded_depth_limit", didExceedLimit(BreakDimension.DEPTH))
      .withVariable("slot_index", slotIndex)
      .withVariable("slot_count", parametersSlots.parametersBySlotIndex.size())
      .withVariable("slot_enabled", parametersSlots.getSelectedSlotIndex() == slotIndex)
      .withVariable("is_locked", locked);
  }

  public boolean doesMaterialMismatchFilter(Material material) {
    if (filter == null || !filterEnabled)
      return false;

    var itemType = material.asItemType();

    if (itemType == null)
      return false;

    return !filter.predicate.test(itemType.createItemStack());
  }

  public void removeFilter(String commandLabel, TranslationLanguage currentLanguage) {
    if (tellIfLocked())
      return;

    if (filter == null) {
      parametersSlots.config.rootSection.multiBreak.noFilterSet.sendMessage(parametersSlots.player, makeEnvironment());
      return;
    }

    parametersSlots.config.rootSection.multiBreak.filterRemoved.sendMessage(
      parametersSlots.player,
      makeEnvironment()
        .withVariable("set_command", makeFilterSetCommand(commandLabel, currentLanguage))
    );

    filter = null;
    filterEnabled = false;
  }

  public @Nullable String makeFilterSetCommand(String label, TranslationLanguage currentLanguage) {
    if (filter == null)
      return null;

    if (currentLanguage == filter.language)
      return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER) + " " + filter.getTokenPredicateString();

    return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER_WITH_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(filter.language) + " " + filter.getTokenPredicateString();
  }

  public void setFilterEnabled(@Nullable Boolean value) {
    if (tellIfLocked())
      return;

    if (filter == null) {
      parametersSlots.config.rootSection.multiBreak.noFilterSet.sendMessage(parametersSlots.player, makeEnvironment());
      return;
    }

    if (value != null) {
      if (filterEnabled == value) {
        if (filterEnabled) {
          parametersSlots.config.rootSection.multiBreak.filterAlreadyEnabled.sendMessage(parametersSlots.player, makeEnvironment());
          return;
        }

        parametersSlots.config.rootSection.multiBreak.filterAlreadyDisabled.sendMessage(parametersSlots.player, makeEnvironment());
        return;
      }

      filterEnabled = value;
    }
    else
      filterEnabled ^= true;

    if (filterEnabled) {
      parametersSlots.config.rootSection.multiBreak.filterNowEnabled.sendMessage(parametersSlots.player, makeEnvironment());
      return;
    }

    parametersSlots.config.rootSection.multiBreak.filterNowDisabled.sendMessage(parametersSlots.player, makeEnvironment());
  }

  public void toggleLocked() {
    locked ^= true;

    if (locked) {
      parametersSlots.config.rootSection.multiBreak.slotNowLocked.sendMessage(parametersSlots.player, makeEnvironment());
      return;
    }

    parametersSlots.config.rootSection.multiBreak.slotNowUnlocked.sendMessage(parametersSlots.player, makeEnvironment());
  }

  public boolean tellIfLocked() {
    if (locked) {
      parametersSlots.config.rootSection.multiBreak.slotIsLocked.sendMessage(parametersSlots.player, makeEnvironment());
      return true;
    }

    return false;
  }
}
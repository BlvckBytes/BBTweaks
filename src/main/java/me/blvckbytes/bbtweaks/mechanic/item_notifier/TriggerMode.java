package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public enum TriggerMode {

  ADD {
    @Override
    public @Nullable TriggerData getDataIfTriggered(SlotData[] previous, SlotData[] current, @Nullable ItemPredicate predicate) {
      if (previous.length != current.length)
        return null;

      for (var index = 0; index < previous.length; ++index) {
        var previousSlot = previous[index];
        var currentSlot = current[index];

        if (currentSlot.itemType().isAir())
          continue;

        if (
          // Swapped out stack or added to an AIR slot
          previousSlot.itemType() != currentSlot.itemType()
            // Increased amount of stack
            || previousSlot.amount() < currentSlot.amount()
        ) {
          if (passesPredicate(currentSlot, predicate))
            return new TriggerData(index, currentSlot.itemType());
        }
      }

      return null;
    }
  },

  REMOVE {
    @Override
    public @Nullable TriggerData getDataIfTriggered(SlotData[] previous, SlotData[] current, @Nullable ItemPredicate predicate) {
      if (previous.length != current.length)
        return null;

      for (var index = 0; index < previous.length; ++index) {
        var previousSlot = previous[index];
        var currentSlot = current[index];

        if (previousSlot.itemType().isAir())
          continue;

        if (
          // Removed whole stack
          currentSlot.itemType().isAir()
            // Swapped out two stacks
            || previousSlot.itemType() != currentSlot.itemType()
            // Decreased amount of stack
            || previousSlot.amount() > currentSlot.amount()
        ) {
          if (passesPredicate(previousSlot, predicate))
            return new TriggerData(index, previousSlot.itemType());
        }
      }

      return null;
    }
  },

  FULL_SLOTS {
    @Override
    public @Nullable TriggerData getDataIfTriggered(SlotData[] previous, SlotData[] current, @Nullable ItemPredicate predicate) {
      if (previous.length != current.length)
        return null;

      if (areAllFull(previous, false))
        return null;

      if (areAllFull(current, false))
        return new TriggerData(-1, Material.AIR);

      return null;
    }
  },

  FULL_STACKS {
    @Override
    public @Nullable TriggerData getDataIfTriggered(SlotData[] previous, SlotData[] current, @Nullable ItemPredicate predicate) {
      if (previous.length != current.length)
        return null;

      if (areAllFull(previous, true))
        return null;

      if (areAllFull(current, true))
        return new TriggerData(-1, Material.AIR);

      return null;
    }
  },

  EMPTY {
    @Override
    public @Nullable TriggerData getDataIfTriggered(SlotData[] previous, SlotData[] current, @Nullable ItemPredicate predicate) {
      if (previous.length != current.length)
        return null;

      if (areAllEmpty(previous))
        return null;

      if (areAllEmpty(current))
        return new TriggerData(-1, Material.AIR);

      return null;
    }
  },
  ;

  public abstract @Nullable TriggerData getDataIfTriggered(
    SlotData[] previous,
    SlotData[] current,
    @Nullable ItemPredicate predicate
  );

  private static boolean areAllEmpty(SlotData[] dataArray) {
    for (var slotData : dataArray) {
      if (!(slotData.itemType().isAir() || slotData.amount() <= 0))
        return false;
    }

    return true;
  }

  private static boolean areAllFull(SlotData[] dataArray, boolean maxStackSize) {
    for (var slotData : dataArray) {
      if (slotData.itemType().isAir())
        return false;

      if (maxStackSize) {
        var stackSize = slotData.itemType().getMaxStackSize();

        if (slotData.amount() < stackSize)
          return false;

        continue;
      }

      if (slotData.amount() == 0)
        return false;
    }

    return true;
  }

  private static boolean passesPredicate(SlotData data, @Nullable ItemPredicate predicate) {
    if (predicate == null)
      return true;

    return predicate.test(new ItemStack(data.itemType()));
  }
}

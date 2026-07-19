package me.blvckbytes.bbtweaks.pipes;

import me.blvckbytes.bbtweaks.util.ItemUtil;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PipeItems {

  private static class ItemAndOriginSlot {
    private final int originSlot;
    private @Nullable ItemStack item;

    private ItemAndOriginSlot(int originSlot, @NotNull ItemStack item) {
      this.originSlot = originSlot;
      this.item = item;
    }
  }

  private interface ActiveItemIterationHandler {
    boolean handleAndGetIfContinue(int contentIndex, ItemAndOriginSlot itemAndOriginSlot);
  }

  private final List<ItemAndOriginSlot> contents;
  private long filteredOutContentIndices;
  private final MutableInt reducedContentIndex;

  private PipeItems(
    List<ItemAndOriginSlot> contents,
    long filteredOutContentIndices,
    MutableInt reducedContentIndex
  ) {
    this.contents = contents;
    this.filteredOutContentIndices = filteredOutContentIndices;
    this.reducedContentIndex = reducedContentIndex;
  }

  public PipeItems() {
    this(new ArrayList<>(), 0, new MutableInt(-1));
  }

  public boolean addIfNonDuplicate(int originSlot, @NotNull ItemStack item) {
    if (!ItemUtil.isStackValid(item))
      return false;

    // Unreachable in day-to-day scenarios, as a double-chest has 54 slots and
    // even putting this many different items in there is highly unlikely.
    // Ensures that we can use a long to create a filtered-out indices bit-set.
    if (contents.size() >= Long.SIZE)
      return false;

    for (var itemAndSlot : contents) {
      if (item.isSimilar(itemAndSlot.item))
        return false;
    }

    contents.add(new ItemAndOriginSlot(originSlot, item));
    return true;
  }

  public boolean isEmptyOrNoneActive() {
    return !forEachActiveItemAndGetIfAny((_, _) -> false);
  }

  public void forEachActiveItemAndBreakAfterReduce(PipeItemReduceHandler handler) {
    forEachActiveItemAndGetIfAny((contentIndex, itemAndSlot) -> {
      assert itemAndSlot.item != null;

      var previousAmount = itemAndSlot.item.getAmount();
      var remainingAmount = handler.handleAndGetRemainingAmount(itemAndSlot.originSlot, itemAndSlot.item);
      var newAmount = Math.min(previousAmount, remainingAmount);

      if (newAmount == previousAmount)
        return true;

      itemAndSlot.item.setAmount(newAmount);
      reducedContentIndex.value = contentIndex;

      if (newAmount <= 0)
        itemAndSlot.item = null;

      return false;
    });
  }

  public void forEachRemainingItem(PipeItemViewHandler handler) {
    for (var itemAndSlot : contents) {
      if (ItemUtil.isStackValid(itemAndSlot.item))
        handler.handle(itemAndSlot.originSlot, itemAndSlot.item);
    }
  }

  public PipeItems filterAndMakeSub(Predicate<ItemStack> predicate) {
    var subItems = new PipeItems(contents, filteredOutContentIndices, reducedContentIndex);

    forEachActiveItemAndGetIfAny((contentIndex, itemAndOriginSlot) -> {
      if (itemAndOriginSlot.item != null && !predicate.test(itemAndOriginSlot.item))
        subItems.filteredOutContentIndices |= 1L << contentIndex;

      return true;
    });

    return subItems;
  }

  private boolean forEachActiveItemAndGetIfAny(ActiveItemIterationHandler handler) {
    var reducedIndex = reducedContentIndex.value;

    if (reducedIndex >= 0) {
      if ((filteredOutContentIndices & (1L << reducedIndex)) != 0)
        return false;

      var reducedItem = contents.get(reducedIndex);

      if (reducedItem.item != null) {
        handler.handleAndGetIfContinue(reducedIndex, reducedItem);
        return true;
      }

      return false;
    }

    var encounteredAnyActive = false;

    for (var index = 0; index < contents.size(); ++index) {
      if ((filteredOutContentIndices & (1L << index)) != 0)
        continue;

      var itemAndSlot = contents.get(index);

      if (itemAndSlot.item == null)
        continue;

      encounteredAnyActive = true;

      if (!handler.handleAndGetIfContinue(index, itemAndSlot))
        break;
    }

    return encounteredAnyActive;
  }
}

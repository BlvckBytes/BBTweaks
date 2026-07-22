package me.blvckbytes.bbtweaks.mechanic.util;

import me.blvckbytes.bbtweaks.mechanic.common.TransferCounters;
import me.blvckbytes.bbtweaks.util.MutableInt;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

public class InventoryUtil {

  public static void causeBlockUpdates(Block mountBlock, Inventory inventory) {
    mountBlock.getState().update(true, true);

    if (inventory instanceof DoubleChestInventory doubleChestInventory) {
      if (doubleChestInventory.getRightSide().getHolder() instanceof Container rightContainer) {
        if (!mountBlock.equals(rightContainer.getBlock())) {
          rightContainer.update(true, true);
          return;
        }
      }

      if (doubleChestInventory.getLeftSide().getHolder() instanceof Container leftContainer) {
        if (!mountBlock.equals(leftContainer.getBlock()))
          leftContainer.update(true, true);
      }
    }
  }

  public static boolean tryMoveItemsAndGetIfAny(
    ItemStack[] items,
    Inventory targetInventory,
    TransferCounters counters,
    @Nullable IntPredicate slotPredicate,
    @Nullable ItemPredicate itemPredicate
  ) {
    var movedAnyItems = false;

    for (var slotIndex = 0; slotIndex < items.length; ++slotIndex) {
      if (slotPredicate != null && !slotPredicate.test(slotIndex))
        continue;

      var currentItem = items[slotIndex];

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      var initialAmount = currentItem.getAmount();

      if (initialAmount <= 0)
        continue;

      if (itemPredicate != null && !itemPredicate.test(currentItem)) {
        counters.encounteredFilterMismatch = true;
        continue;
      }

      var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(currentItem, currentItem.getAmount(), targetInventory);

      var movedAmount = initialAmount - remainingAmount;

      if (movedAmount > 0) {
        counters.totalTransferredCountByType.computeIfAbsent(currentItem.getType(), _ -> new MutableInt()).value += movedAmount;
        movedAnyItems = true;
      }

      if (remainingAmount > 0) {
        counters.totalExcessCountByType.computeIfAbsent(currentItem.getType(), _ -> new MutableInt()).value += remainingAmount;
        currentItem.setAmount(remainingAmount);
        continue;
      }

      items[slotIndex] = null;
    }

    return movedAnyItems;
  }

  public static int addItemToInventoryAndGetRemainingAmount(ItemStack itemToAdd, int amount, Inventory inventory) {
    var firstVacantSlotIndex = -1;
    var remainingAmount = amount;

    var inventorySize = inventory.getSize();

    // Only add to storage-contents of the player-inventory.
    if (inventory instanceof PlayerInventory)
      inventorySize = Math.min(inventorySize, 9 * 4);

    // 1. Fill up all partial stacks

    for (var slotIndex = 0; slotIndex < inventorySize; ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        if (firstVacantSlotIndex < 0)
          firstVacantSlotIndex = slotIndex;

        continue;
      }

      if (!itemToAdd.isSimilar(currentItem))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      var amountToAdd = Math.min(remainingAmount, remainingSpace);

      currentItem.setAmount(currentItem.getAmount() + amountToAdd);

      remainingAmount -= amountToAdd;

      if (remainingAmount <= 0)
        return 0;
    }

    if (remainingAmount <= 0)
      return 0;

    // There's no need to once more scan for more vacant slots, as there aren't any left.
    if (firstVacantSlotIndex < 0)
      return remainingAmount;

    // 2. Since there's still some left to add, put as much as possible into the first vacant
    //    slot we've discovered while scanning for partial stacks.

    var remainder = new ItemStack(itemToAdd);

    var remainderAmount = Math.min(itemToAdd.getMaxStackSize(), remainingAmount);
    remainder.setAmount(remainderAmount);

    inventory.setItem(firstVacantSlotIndex, remainder);
    remainingAmount -= remainderAmount;

    if (remainingAmount <= 0)
      return 0;

    // 3. If there's still more, we need to fill up more than one vacant slot. Seeing how that's
    //    a rather seldom, special case, we don't keep a list of vacant slots but rather just
    //    iterate again - that's plenty fast.

    for (var slotIndex = 0; slotIndex < inventorySize; ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        remainder = new ItemStack(itemToAdd);

        remainderAmount = Math.min(itemToAdd.getMaxStackSize(), remainingAmount);
        remainder.setAmount(remainderAmount);

        inventory.setItem(slotIndex, remainder);
        remainingAmount -= remainderAmount;

        if (remainingAmount <= 0)
          return 0;
      }
    }

    return remainingAmount;
  }
}

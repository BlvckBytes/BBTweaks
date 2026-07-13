package me.blvckbytes.bbtweaks.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AddOnlyInventory {

  private static final @Nullable ItemStack[] unitStackByMaterialOrdinal;

  static {
    var materialValues = Material.values();

    unitStackByMaterialOrdinal = new ItemStack[materialValues.length];

    for (var index = 0; index < materialValues.length; ++index) {
      var material = materialValues[index];

      if (material.isItem())
        unitStackByMaterialOrdinal[index] = new ItemStack(material);
    }
  }

  // NOTE: We intentionally *never* store an item by reference, as to absolutely avoid
  //       untracked mutations. This is far more important than the little allocation.

  protected final ItemStack[] inventoryContents;
  private final @Nullable PerSlotItemAdditionHandler perSlotItemAdditionHandler;
  private final @Nullable PerCallItemAdditionHandler perCallItemAdditionHandler;

  protected AddOnlyInventory(
    ItemStack[] inventoryContents,
    @Nullable PerSlotItemAdditionHandler perSlotItemAdditionHandler,
    @Nullable PerCallItemAdditionHandler perCallItemAdditionHandler
  ) {
    this.inventoryContents = inventoryContents;
    this.perSlotItemAdditionHandler = perSlotItemAdditionHandler;
    this.perCallItemAdditionHandler = perCallItemAdditionHandler;
  }

  public abstract boolean isSlotDisabled(int slot);

  public int getSize() {
    return inventoryContents.length;
  }

  public int addItemAndGetAddedAmount(Material typeToAdd, int amountToAdd) {
    var unitStack = unitStackByMaterialOrdinal[typeToAdd.ordinal()];

    if (unitStack == null)
      return 0;

    return addItemAndGetAddedAmount(unitStack, amountToAdd);
  }

  public int addItemAndGetAddedAmount(ItemStack itemToAdd, int amountToAdd) {
    var remainingAmount = addItemToInventoryAndGetRemainingAmount(itemToAdd, amountToAdd);
    var addedAmount = amountToAdd - remainingAmount;

    if (perCallItemAdditionHandler != null)
      perCallItemAdditionHandler.onAdditionPerAddCall(itemToAdd, addedAmount, 0);

    return addedAmount;
  }

  public int addItemToSlotAndGetAddedAmount(int slot, ItemStack itemToAdd, int amountToAdd) {
    return addItemToSlotAndGetAddedAmount(slot, itemToAdd, amountToAdd, 0);
  }

  public int addItemToSlotAndGetAddedAmount(int slot, ItemStack itemToAdd, int amountToAdd, int stackSizeOverride) {
    var currentItem = inventoryContents[slot];

    var finalStackSize = itemToAdd.getMaxStackSize();

    if (stackSizeOverride > 0)
      finalStackSize = stackSizeOverride;

    if (!ItemUtil.isStackValid(currentItem)) {
      amountToAdd = Math.min(amountToAdd, finalStackSize);

      var newItem = new ItemStack(itemToAdd);
      newItem.setAmount(amountToAdd);

      inventoryContents[slot] = newItem;

      if (perCallItemAdditionHandler != null)
        perCallItemAdditionHandler.onAdditionPerAddCall(newItem, amountToAdd, stackSizeOverride);

      if (perSlotItemAdditionHandler != null)
        perSlotItemAdditionHandler.onAdditionPerSlot(slot, true, newItem, amountToAdd, stackSizeOverride);

      return amountToAdd;
    }

    if (!currentItem.isSimilar(itemToAdd))
      return 0;

    var remainingSpace = finalStackSize - currentItem.getAmount();

    if (remainingSpace <= 0)
      return 0;

    var addedAmount = Math.min(remainingSpace, amountToAdd);

    currentItem.setAmount(currentItem.getAmount() + addedAmount);

    if (perCallItemAdditionHandler != null)
      perCallItemAdditionHandler.onAdditionPerAddCall(itemToAdd, addedAmount, stackSizeOverride);

    if (perSlotItemAdditionHandler != null)
      perSlotItemAdditionHandler.onAdditionPerSlot(slot, false, itemToAdd, addedAmount, stackSizeOverride);

    return addedAmount;
  }

  public @Nullable Integer getAmountIfIsSimilarOrVacant(int slot, ItemStack item) {
    var currentItem = inventoryContents[slot];

    if (!ItemUtil.isStackValid(currentItem))
      return 0;

    if (!currentItem.isSimilar(item))
      return null;

    return currentItem.getAmount();
  }

  // TODO: This is an adaptation of the same method in InventoryUtil - maybe we can migrate its usage-sites some day.
  private int addItemToInventoryAndGetRemainingAmount(ItemStack itemToAdd, int amount) {
    var firstVacantSlotIndex = -1;
    var remainingAmount = amount;

    // 1. Fill up all partial stacks

    for (var slotIndex = 0; slotIndex < inventoryContents.length; ++slotIndex) {
      var currentItem = inventoryContents[slotIndex];

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

      if (perSlotItemAdditionHandler != null)
        perSlotItemAdditionHandler.onAdditionPerSlot(slotIndex, false, itemToAdd, amountToAdd, 0);

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

    inventoryContents[firstVacantSlotIndex] = remainder;

    if (perSlotItemAdditionHandler != null)
        perSlotItemAdditionHandler.onAdditionPerSlot(firstVacantSlotIndex, true, remainder, remainderAmount, 0);

    remainingAmount -= remainderAmount;

    if (remainingAmount <= 0)
      return 0;

    // 3. If there's still more, we need to fill up more than one vacant slot. Seeing how that's
    //    a rather seldom, special case, we don't keep a list of vacant slots but rather just
    //    iterate again - that's plenty fast.

    for (var slotIndex = 0; slotIndex < inventoryContents.length; ++slotIndex) {
      var currentItem = inventoryContents[slotIndex];

      if (currentItem == null || currentItem.getType().isAir()) {
        remainder = new ItemStack(itemToAdd);

        remainderAmount = Math.min(itemToAdd.getMaxStackSize(), remainingAmount);
        remainder.setAmount(remainderAmount);

        inventoryContents[slotIndex] = remainder;

        if (perSlotItemAdditionHandler != null)
          perSlotItemAdditionHandler.onAdditionPerSlot(slotIndex, true, remainder, remainderAmount, 0);

        remainingAmount -= remainderAmount;

        if (remainingAmount <= 0)
          return 0;
      }
    }

    return remainingAmount;
  }
}

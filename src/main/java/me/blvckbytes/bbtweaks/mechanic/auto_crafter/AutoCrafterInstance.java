package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.SimulatingAddOnlyInventory;
import me.blvckbytes.bbtweaks.util.SlotItemAddition;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;

public class AutoCrafterInstance extends SISOInstance {

  private static final int MATRIX_SIZE = 9;

  private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

  private final EnumSet<AutoCrafterFlag> flags;
  private final RecipeCache recipeCache;

  private @Nullable CachedRecipe cachedRecipe;
  private long cachedRecipeMatrixMsb;
  private long cachedRecipeMatrixLsb;
  private TriState wasMatrixInvalid = TriState.NULL;

  public AutoCrafterInstance(Sign sign, EnumSet<AutoCrafterFlag> flags, RecipeCache recipeCache) {
    super(sign);

    this.flags = flags;
    this.recipeCache = recipeCache;
  }

  @Override
  public boolean tick(long time) {
    if (time % 2 != 0)
      return true;

    if (!isBlockLoaded(getMountBlock()))
      return true;

    var blockData = getMountBlock().getBlockData();

    if (!(blockData instanceof org.bukkit.block.data.type.Crafter crafterData))
      return false;

    // Allows to temporarily deactivate the crafter by powering it.
    if (crafterData.isTriggered())
      return true;

    var outputFace = switch (crafterData.getOrientation()) {
      case DOWN_NORTH, DOWN_EAST, DOWN_SOUTH, DOWN_WEST -> BlockFace.DOWN;
      case UP_NORTH, UP_EAST, UP_SOUTH, UP_WEST -> BlockFace.UP;
      case WEST_UP -> BlockFace.WEST;
      case EAST_UP -> BlockFace.EAST;
      case NORTH_UP -> BlockFace.NORTH;
      case SOUTH_UP -> BlockFace.SOUTH;
    };

    var containerBlock = getMountBlock().getRelative(outputFace);

    if (!BlockUtil.areAllContainerBlocksLoaded(containerBlock, blockData))
      return true;

    if (!(getMountBlock().getState(false) instanceof Crafter crafterState))
      return false;

    Inventory outputInventory = null;

    if (containerBlock.getState(false) instanceof Container container)
      outputInventory = container.getInventory();

    craft(crafterState, containerBlock, outputInventory);
    return true;
  }

  private long getSlotTypeOrdinal(ItemStack[] matrixContents, int slot) {
    ItemStack item;

    if (slot < 0 || slot >= matrixContents.length || !isStackValid(item = matrixContents[slot]))
      return Material.AIR.ordinal();

    return item.getType().ordinal();
  }

  private long computeMatrixMsb(ItemStack[] matrixContents) {
    return (
      getSlotTypeOrdinal(matrixContents, 5)
        | (getSlotTypeOrdinal(matrixContents, 6) << 12)
        | (getSlotTypeOrdinal(matrixContents, 7) << (12 * 2))
        | (getSlotTypeOrdinal(matrixContents, 8) << (12 * 3))
    );
  }

  private long computeMatrixLsb(ItemStack[] matrixContents) {
    return (
      getSlotTypeOrdinal(matrixContents, 0)
        | (getSlotTypeOrdinal(matrixContents, 1) << 12)
        | (getSlotTypeOrdinal(matrixContents, 2) << (12 * 2))
        | (getSlotTypeOrdinal(matrixContents, 3) << (12 * 3))
        | (getSlotTypeOrdinal(matrixContents, 4) << (12 * 4))
    );
  }

  private void tryRecomputeCachedRecipe(ItemStack[] matrixContents) {
    var priorRecipeMatrixMsb = this.cachedRecipeMatrixMsb;
    var priorRecipeMatrixLsb = this.cachedRecipeMatrixLsb;

    this.cachedRecipeMatrixMsb = computeMatrixMsb(matrixContents);
    this.cachedRecipeMatrixLsb = computeMatrixLsb(matrixContents);

    // Do not retry malformed matrix-constellations over and over again - remember the failure and only retry after a reconfiguration.
    if (wasMatrixInvalid == TriState.TRUE && priorRecipeMatrixMsb == cachedRecipeMatrixMsb && priorRecipeMatrixLsb == cachedRecipeMatrixLsb)
      return;

    for (var cachedRecipe : recipeCache.getRecipes()) {
      if (!isMatrixSatisfyingRecipe(matrixContents, cachedRecipe))
        continue;

      this.cachedRecipe = cachedRecipe;
      this.wasMatrixInvalid = TriState.FALSE;
      return;
    }

    this.cachedRecipe = null;
    this.wasMatrixInvalid = TriState.TRUE;
  }

  private void craft(Crafter crafter, Block outputBlock, @Nullable Inventory outputInventory) {
    var crafterInventory = crafter.getInventory();
    var matrixContents = crafterInventory.getContents();

    var hasVacantSlots = false;
    var nonDisabledSlotCount = 0;
    var totalAmount = 0;

    var maxItemAmount = -1;
    var maxAmountSlot = -1;

    for (var matrixSlot = 0; matrixSlot < matrixContents.length; ++matrixSlot) {
      if (crafter.isSlotDisabled(matrixSlot))
        continue;

      ++nonDisabledSlotCount;

      var matrixItem = matrixContents[matrixSlot];

      if (!isStackValid(matrixItem)) {
        hasVacantSlots = true;
        continue;
      }

      var itemAmount = matrixItem.getAmount();

      if (itemAmount > maxItemAmount) {
        maxItemAmount = itemAmount;
        maxAmountSlot = matrixSlot;
      }

      totalAmount += itemAmount;

      if (!flags.contains(AutoCrafterFlag.USE_SLOT_STATE_AS_PATTERN)) {
        if (itemAmount < 2)
          return;
      }
    }

    if (nonDisabledSlotCount == 0)
      return;

    if (hasVacantSlots) {
      // We always expect there to be an item in a non-disabled slot before crafting,
      // as to avoid producing undesired results with other partial recipes.
      if (!flags.contains(AutoCrafterFlag.USE_SLOT_STATE_AS_PATTERN))
        return;

      // Try to redistribute currently available items as to avoid stalling.

      // Not enough to redistribute.
      if (totalAmount < nonDisabledSlotCount || maxAmountSlot < 0)
        return;

      // Distribute the biggest stack into the inventory.

      var maxAmountItem = crafterInventory.getItem(maxAmountSlot);

      if (!isStackValid(maxAmountItem))
        return;

      crafterInventory.setItem(maxAmountSlot, null);

      var remainingAmount = distributeIntoCrafterToMakeEvenAndGetRemainingAmount(crafter, maxAmountItem);

      // Unreachable, seeing how we're just redistributing one slot at a time, and it should always
      // fit back into itself in the worst case, plus we've ensured that there are other vacant ones.
      if (remainingAmount > 0) {
        var remainderStack = new ItemStack(maxAmountItem);
        remainderStack.setAmount(remainingAmount);
        var crafterBlock = crafter.getBlock();
        crafterBlock.getWorld().dropItem(crafterBlock.getLocation(), remainderStack);
      }

      // Always retry after redistribution, as it may be necessary to carry out multiple times.
      // This way, we also re-fetch the matrix-contents and re-evaluate the rules above.
      return;
    }

    if (cachedRecipe == null)
      tryRecomputeCachedRecipe(matrixContents);

    if (cachedRecipe == null)
      return;

    var didMatrixChange = cachedRecipeMatrixMsb != computeMatrixMsb(matrixContents) || cachedRecipeMatrixLsb != computeMatrixLsb(matrixContents);

    if (didMatrixChange && !isMatrixSatisfyingRecipe(matrixContents, cachedRecipe)) {
      tryRecomputeCachedRecipe(matrixContents);

      if (cachedRecipe == null)
        return;
    }

    var recipeResultItems = new ArrayList<ItemStack>();

    recipeResultItems.add(cachedRecipe.getResultCopy());

    for (int index = 0; index < MATRIX_SIZE; index++) {
      var matrixItem = crafterInventory.getItem(index);

      if (matrixItem == null)
        continue;

      var typeAfterUse = switch (matrixItem.getType()) {
        case WATER_BUCKET, LAVA_BUCKET, MILK_BUCKET -> Material.BUCKET;
        case HONEY_BOTTLE -> Material.GLASS_BOTTLE;
        default -> null;
      };

      if (typeAfterUse != null)
        recipeResultItems.add(new ItemStack(typeAfterUse, 1));
    }

    // If a container is attached, crafting becomes a transaction - either all results fit, or we stall.
    if (outputInventory != null) {
      var additions = new ArrayList<SlotItemAddition>();

      var simulatingInventory = new SimulatingAddOnlyInventory(
        outputInventory,
        (slot, wasVacant, addedItem, addedAmount, stackSizeOverride) -> additions.add(new SlotItemAddition(slot, wasVacant, addedItem, addedAmount, stackSizeOverride)),
        null
      );

      for (var result : recipeResultItems) {
        var amountToAdd = result.getAmount();
        var addedAmount = simulatingInventory.addItemAndGetAddedAmount(result, amountToAdd);

        // Stall crafting if the results have no more space.
        if (addedAmount < amountToAdd)
          return;
      }

      for (var addition : additions) {
        // Theoretically unreachable, if nobody modified the inventory in the mean-time.
        if (!addition.performIfCanFit(outputInventory))
          dropItem(outputBlock, addition.makeStack());
      }
    }

    else {
      for (var result : recipeResultItems)
        dropItem(outputBlock, result);
    }

    for (int index = 0; index < MATRIX_SIZE; index++) {
      var matrixItem = crafterInventory.getItem(index);

      if (matrixItem == null)
        continue;

      if (matrixItem.getAmount() <= 1) {
        crafterInventory.setItem(index, null);
        continue;
      }

      matrixItem.setAmount(matrixItem.getAmount() - 1);
    }
  }

  private void dropItem(Block outputBlock, ItemStack item) {
    var world = outputBlock.getWorld();
    var location = outputBlock.getLocation().add(.5, .5, .5);
    world.dropItem(location, item);
  }

  private static boolean doesRecipeMatchAtOffset(
    CachedShapedRecipe recipe,
    int rowOffset, int columnOffset, boolean mirrorHorizontally,
    ItemStack[] matrixContents
  ) {
    for (int rowIndex = 0; rowIndex < 3; ++rowIndex) {
      for (int columnIndex = 0; columnIndex < 3; ++columnIndex) {
        var matrixItem = matrixContents[columnIndex + rowIndex * 3];

        // Recipes are always trimmed and aligned to the top left corner, i.e. (0, 0). If we now seek
        // to slide the window of the choices-matrix, all slots prior to the offset need to be vacant.
        if (rowIndex < rowOffset || columnIndex < columnOffset) {
          if (isStackValid(matrixItem))
            return false;

          continue;
        }

        var targetColumn = columnIndex - columnOffset;

        if (mirrorHorizontally)
          targetColumn = (recipe.width - 1) - targetColumn;

        var choice = recipe.getChoiceAt(rowIndex - rowOffset, targetColumn);

        // The shaped recipe has a hole at this location, meaning we expect a vacant slot.
        if (choice == null) {
          if (isStackValid(matrixItem))
            return false;

          continue;
        }

        if (!choice.test(matrixItem == null ? AIR_STACK : matrixItem))
          return false;
      }
    }

    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isMatrixSatisfyingRecipe(ItemStack[] matrixContents, CachedRecipe cachedRecipe) {
    if (cachedRecipe instanceof CachedShapedRecipe shapedRecipe) {
      for (int rowOffset = 0; rowOffset <= 3 - shapedRecipe.height; ++rowOffset) {
        for (int columnOffset = 0; columnOffset <= 3 - shapedRecipe.width; ++columnOffset) {
          if (doesRecipeMatchAtOffset(shapedRecipe, rowOffset, columnOffset, false, matrixContents))
            return true;

          if (shapedRecipe.horizontallyAsymmetrical) {
            if (doesRecipeMatchAtOffset(shapedRecipe, rowOffset, columnOffset, true, matrixContents))
              return true;
          }
        }
      }

      return false;
    }

    if (cachedRecipe instanceof CachedShapelessRecipe shapelessRecipe) {
      var remainingIngredients = new ArrayList<>(shapelessRecipe.getChoiceList());

      // If it's empty already, something is wrong with the recipe.
      if (remainingIngredients.isEmpty())
        return false;

      for (ItemStack matrixItem : matrixContents) {
        if (!isStackValid(matrixItem))
          continue;

        // No more required ingredients left, but there are still additional items in the crafting-matrix => mismatch.
        if (remainingIngredients.isEmpty())
          return false;

        for (var iterator = remainingIngredients.iterator(); iterator.hasNext();) {
          var requiredIngredient = iterator.next();

          if (requiredIngredient.test(matrixItem)) {
            iterator.remove();
            break;
          }
        }
      }

      return remainingIngredients.isEmpty();
    }

    return false;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private static boolean isStackValid(ItemStack item) {
    if (item == null)
      return false;

    if (item.getType().isAir())
      return false;

    return item.getAmount() > 0;
  }

  // ================================================================================
  // Forked from my CB3 pipe-implementation - not ideal, as it's a lot of code-duplication.

  // We want to have bottles, buckets, etc. stack as efficiently as possible, such that
  // the crafter can operate at its full speed without having to await another pipe-refill.
  private static final int CRAFTER_MAX_STACK_SIZE = 64;

  private static int distributeIntoCrafterToMakeEvenAndGetRemainingAmount(Crafter crafter, ItemStack itemToAdd) {
    var inventory = crafter.getInventory();
    var inventorySize = inventory.getSize();

    var spaceByIndex = new int[inventorySize];
    var addedAmountByIndex = new int[inventorySize];

    for (var index = 0; index < inventorySize; ++index) {
      if (crafter.isSlotDisabled(index))
        continue;

      var currentItem = inventory.getItem(index);

      if (!isStackValid(currentItem)) {
        spaceByIndex[index] = CRAFTER_MAX_STACK_SIZE;
        continue;
      }

      if (!itemToAdd.isSimilar(currentItem))
        continue;

      var currentAmount = currentItem.getAmount();
      var currentSpace = CRAFTER_MAX_STACK_SIZE - currentAmount;

      if (currentSpace <= 0)
        continue;

      spaceByIndex[index] = currentSpace;
    }

    var simulatedRemainingAmount = itemToAdd.getAmount();

    while (simulatedRemainingAmount > 0) {
      var maxSpace = 0;
      var maxSpaceIndex = -1;

      for (var index = 0; index < spaceByIndex.length; ++index) {
        var currentSpace = spaceByIndex[index];

        if (currentSpace <= 0)
          continue;

        if (maxSpaceIndex < 0 || currentSpace > maxSpace) {
          maxSpaceIndex = index;
          maxSpace = currentSpace;
        }
      }

      if (maxSpaceIndex < 0)
        break;

      ++addedAmountByIndex[maxSpaceIndex];
      --spaceByIndex[maxSpaceIndex];
      --simulatedRemainingAmount;
    }

    var actualRemainingAmount = itemToAdd.getAmount();

    for (var index = 0; index < addedAmountByIndex.length; ++index) {
      var simulatedAddedAmount = addedAmountByIndex[index];

      if (simulatedAddedAmount <= 0)
        continue;

      var currentItem = inventory.getItem(index);

      if (!isStackValid(currentItem)) {
        var newItem = new ItemStack(itemToAdd);
        newItem.setAmount(simulatedAddedAmount);
        inventory.setItem(index, newItem);
        actualRemainingAmount -= simulatedAddedAmount;
        continue;
      }

      if (!currentItem.isSimilar(itemToAdd))
        continue;

      var remainingSpace = CRAFTER_MAX_STACK_SIZE - currentItem.getAmount();

      if (remainingSpace <= 0)
        return 0;

      var addedAmount = Math.min(remainingSpace, simulatedAddedAmount);

      currentItem.setAmount(currentItem.getAmount() + addedAmount);

      actualRemainingAmount -= addedAmount;

      if (actualRemainingAmount <= 0)
        break;
    }

    return Math.max(0, actualRemainingAmount);
  }
}

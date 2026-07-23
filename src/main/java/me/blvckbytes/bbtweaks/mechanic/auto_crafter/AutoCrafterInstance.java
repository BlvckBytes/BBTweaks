package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import me.blvckbytes.bbtweaks.util.SimulatingAddOnlyInventory;
import me.blvckbytes.bbtweaks.util.SlotItemAddition;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AutoCrafterInstance extends SISOInstance {

  private static final int MATRIX_SIZE = 9;

  private final RecipeCache recipeCache;
  private final MatrixCacheHelper matrixCacheHelper;

  private @Nullable CachedRecipe cachedRecipe;

  public AutoCrafterInstance(
    Sign sign,
    Side side,
    RecipeCache recipeCache
  ) {
    super(sign, side);

    this.recipeCache = recipeCache;
    this.matrixCacheHelper = new MatrixCacheHelper();
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

    if (!BlockUtil.areAllContainerBlocksLoaded(containerBlock, null))
      return true;

    if (!(getMountBlock().getState(false) instanceof Crafter crafterState))
      return false;

    Inventory outputInventory = null;

    if (containerBlock.getState(false) instanceof Container container)
      outputInventory = container.getInventory();

    craft(crafterState, containerBlock, outputInventory);
    return true;
  }

  private void tryRecomputeCachedRecipe(ItemStack[] matrixContents) {
    matrixCacheHelper.runIfMatrixChanged(matrixContents, () -> {
      var mappedContents = MatrixItem.map(matrixContents);

      for (var cachedRecipe : recipeCache.getRecipes()) {
        if (!cachedRecipe.areMatrixContentsSatisfyingRecipe(mappedContents))
          continue;

        this.cachedRecipe = cachedRecipe;
        return;
      }

      this.cachedRecipe = null;
    });
  }

  private void craft(Crafter crafter, Block outputBlock, @Nullable Inventory outputInventory) {
    var crafterInventory = crafter.getInventory();
    var matrixContents = crafterInventory.getContents();

    var hasVacantSlots = false;
    var nonDisabledSlotCount = 0;

    for (var matrixSlot = 0; matrixSlot < matrixContents.length; ++matrixSlot) {
      if (crafter.isSlotDisabled(matrixSlot))
        continue;

      ++nonDisabledSlotCount;

      var matrixItem = matrixContents[matrixSlot];

      if (!ItemUtil.isStackValid(matrixItem)) {
        hasVacantSlots = true;
        continue;
      }

      var itemAmount = matrixItem.getAmount();

      if (itemAmount < 2)
        return;
    }

    if (nonDisabledSlotCount == 0 || hasVacantSlots)
      return;

    if (cachedRecipe == null)
      tryRecomputeCachedRecipe(matrixContents);

    if (cachedRecipe == null)
      return;

    if (matrixCacheHelper.didMatrixChange(matrixContents) && !cachedRecipe.areMatrixContentsSatisfyingRecipe(MatrixItem.map(matrixContents))) {
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

      var typeAfterUse = recipeCache.getEmptyTypeAfterUse(matrixItem.getType());

      if (typeAfterUse != null)
        recipeResultItems.add(new ItemStack(typeAfterUse, 1));
    }

    if (tryStoreOrDropAndGetIfNoSpace(outputBlock, outputInventory, recipeResultItems))
      return;

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

  public static boolean tryStoreOrDropAndGetIfNoSpace(
    Block outputBlock,
    @Nullable Inventory outputInventory,
    List<ItemStack> recipeResultItems
  ) {
    if (outputInventory == null) {
      for (var result : recipeResultItems)
        dropItem(outputBlock, result);

      return false;
    }

    // If a container is attached, crafting becomes a transaction - either all results fit, or we stall.

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
        return true;
    }

    for (var addition : additions) {
      // Theoretically unreachable, if nobody modified the inventory in the mean-time.
      if (!addition.performIfCanFit(outputInventory))
        dropItem(outputBlock, addition.makeStack());
    }

    return false;
  }

  private static void dropItem(Block outputBlock, ItemStack item) {
    var world = outputBlock.getWorld();
    var location = outputBlock.getLocation().add(.5, .5, .5);
    world.dropItem(location, item);
  }
}

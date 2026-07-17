package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import me.blvckbytes.bbtweaks.util.SimulatingAddOnlyInventory;
import me.blvckbytes.bbtweaks.util.SlotItemAddition;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AutoCrafterInstance extends SISOInstance {

  private static final int MATRIX_SIZE = 9;

  private final RecipeCache recipeCache;

  private @Nullable CachedRecipe cachedRecipe;
  private long cachedRecipeMatrixMsb;
  private long cachedRecipeMatrixLsb;
  private TriState wasMatrixInvalid = TriState.NULL;

  public AutoCrafterInstance(Sign sign, RecipeCache recipeCache) {
    super(sign);

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

  private long getSlotTypeOrdinal(ItemStack[] matrixContents, int slot) {
    ItemStack item;

    if (slot < 0 || slot >= matrixContents.length || !ItemUtil.isStackValid(item = matrixContents[slot]))
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
      if (!cachedRecipe.areMatrixContentsSatisfyingRecipe(matrixContents, MatrixItem::new))
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

    var didMatrixChange = cachedRecipeMatrixMsb != computeMatrixMsb(matrixContents) || cachedRecipeMatrixLsb != computeMatrixLsb(matrixContents);

    if (didMatrixChange && !cachedRecipe.areMatrixContentsSatisfyingRecipe(matrixContents, MatrixItem::new)) {
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
}

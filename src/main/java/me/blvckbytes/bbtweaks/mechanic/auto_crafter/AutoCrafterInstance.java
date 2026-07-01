package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;

public class AutoCrafterInstance extends SISOInstance {

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

    craft(crafterState.getInventory(), containerBlock, outputInventory);
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

  private void craft(Inventory crafterInventory, Block outputBlock, @Nullable Inventory outputInventory) {
    var matrixContents = crafterInventory.getContents();

    for (var matrixItem : matrixContents) {
      if (!isStackValid(matrixItem))
        continue;

      if (matrixItem.getAmount() < 2)
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

    for (int index = 0; index < 9; index++) {
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

      if (matrixItem.getAmount() <= 1) {
        crafterInventory.setItem(index, null);
        continue;
      }

      matrixItem.setAmount(matrixItem.getAmount() - 1);
    }

    ArrayList<ItemStack> leftovers;

    if (outputInventory == null)
      leftovers = recipeResultItems;

    else {
      leftovers = new ArrayList<>();

      for (ItemStack stack : recipeResultItems)
        leftovers.addAll(outputInventory.addItem(stack).values());
    }

    if (!leftovers.isEmpty()) {
      var world = outputBlock.getWorld();
      var location = outputBlock.getLocation().add(.5, .5, .5);

      for (var leftover : leftovers)
        world.dropItem(location, leftover);
    }
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
}

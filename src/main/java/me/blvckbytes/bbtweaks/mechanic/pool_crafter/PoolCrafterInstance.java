package me.blvckbytes.bbtweaks.mechanic.pool_crafter;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.AutoCrafterInstance;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.CachedRecipe;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.MatrixCacheHelper;
import me.blvckbytes.bbtweaks.mechanic.auto_crafter.RecipeCache;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PoolCrafterInstance extends SISOInstance {

  private static final int MAX_ATTACHED_CONTAINER_COUNT = 5;

  private static final BlockFace[] DIRECT_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
    BlockFace.UP, BlockFace.DOWN
  };

  private static class InputItem {
    final Inventory inventory;
    final int slotIndex;
    final ItemStack itemStack;

    int useCount;

    private InputItem(Inventory inventory, int slotIndex, ItemStack itemStack) {
      this.inventory = inventory;
      this.slotIndex = slotIndex;
      this.itemStack = itemStack;
    }
  }

  private final RecipeCache recipeCache;
  private MatrixChoices @Nullable [] matrixChoices;
  private final SimilarMaterialsResolver similarMaterialsResolver;

  private final MatrixCacheHelper matrixCacheHelper;
  private final List<CachedRecipe> cachedRecipes;

  public PoolCrafterInstance(
    Sign sign,
    RecipeCache recipeCache,
    SimilarMaterialsResolver similarMaterialsResolver
  ) {
    super(sign);

    this.recipeCache = recipeCache;
    this.similarMaterialsResolver = similarMaterialsResolver;

    this.matrixCacheHelper = new MatrixCacheHelper();
    this.cachedRecipes = new ArrayList<>();
  }

  public List<CachedRecipe> getCachedRecipes() {
    return Collections.unmodifiableList(cachedRecipes);
  }

  @Override
  public boolean tick(long time) {
    if (time % 2 != 0)
      return true;

    if (!isBlockLoaded(mountBlock))
      return true;

    var blockData = mountBlock.getBlockData();

    if (
      blockData.getMaterial() != Material.DROPPER
        || !(blockData instanceof Directional dropperDirectional)
    ) {
      return false;
    }

    var dropperFacing = dropperDirectional.getFacing();
    var inputInventories = new ArrayList<Inventory>();

    for (var directFace : DIRECT_FACES) {
      if (directFace == dropperFacing)
        continue;

      if (tryWalkAttachedDroppersAndPossiblyContainers(mountBlock, directFace, inputInventories))
        break;
    }

    if (!(mountBlock.getState(false) instanceof Container dropperContainer))
      return false;

    var outputBlock = mountBlock.getRelative(dropperFacing);

    Inventory outputInventory = null;

    if (outputBlock.getState(false) instanceof Container outputContainer)
      outputInventory = outputContainer.getInventory();

    craft(
      dropperContainer.getInventory().getContents(),
      inputInventories,
      outputBlock,
      outputInventory
    );

    return true;
  }

  private void craft(
    ItemStack[] matrixContents,
    List<Inventory> inputInventories,
    Block outputBlock,
    @Nullable Inventory outputInventory
  ) {
    tryRecomputeCachedRecipes(matrixContents);

    if (cachedRecipes.isEmpty())
      return;

    if (inputInventories.isEmpty())
      return;

    var inputItemsByMaterial = new EnumMap<Material, List<InputItem>>(Material.class);

    for (var inputInventory : inputInventories) {
      var inventorySize = inputInventory.getSize();

      for (var slotIndex = 0; slotIndex < inventorySize; ++slotIndex) {
        var currentItem = inputInventory.getItem(slotIndex);

        if (!ItemUtil.isStackValid(currentItem))
          continue;

        inputItemsByMaterial
          .computeIfAbsent(currentItem.getType(), _ -> new ArrayList<>())
          .add(new InputItem(inputInventory, slotIndex, currentItem));
      }
    }

    var usedItems = new ArrayList<InputItem>();
    CachedRecipe availableRecipe = null;

    recipeLoop:
    for (var recipe : cachedRecipes) {
      for (var usedItem : usedItems)
        usedItem.useCount = 0;

      usedItems.clear();

      var requiredChoices = recipe.getChoicesForAllSlots();

      choiceLoop:
      for (var choiceIndex = 0; choiceIndex < requiredChoices.size(); ++choiceIndex) {
        var requiredChoice = requiredChoices.get(choiceIndex);

        Material exactMaterial = null;

        if (matrixChoices != null && choiceIndex < matrixChoices.length) {
          var currentMatrixChoices = matrixChoices[choiceIndex];

          if (currentMatrixChoices.exact && currentMatrixChoices.matrixMaterial != null)
            exactMaterial = currentMatrixChoices.matrixMaterial;
        }

        var choices = exactMaterial == null ? requiredChoice.getChoices() : Collections.singletonList(exactMaterial);

        for (var material : choices) {
          var candidateItems = inputItemsByMaterial.get(material);

          if (candidateItems == null)
            continue;

          for (var candidateItem : candidateItems) {
            if (candidateItem.itemStack.getAmount() - candidateItem.useCount <= 0)
              continue;

            if (!usedItems.contains(candidateItem))
              usedItems.add(candidateItem);

            ++candidateItem.useCount;
            continue choiceLoop;
          }
        }

        continue recipeLoop;
      }

      availableRecipe = recipe;
      break;
    }

    if (availableRecipe == null)
      return;

    var recipeResultItems = new ArrayList<ItemStack>();

    recipeResultItems.add(availableRecipe.getResultCopy());

    for (var usedItem : usedItems) {
      var typeAfterUse = recipeCache.getEmptyTypeAfterUse(usedItem.itemStack.getType());

      if (typeAfterUse == null)
        continue;

      for (var i = 0; i < usedItem.useCount; ++i)
        recipeResultItems.add(new ItemStack(typeAfterUse, 1));
    }

    if (AutoCrafterInstance.tryStoreOrDropAndGetIfNoSpace(outputBlock, outputInventory, recipeResultItems))
      return;

    for (var usedItem : usedItems) {
      var currentAmount = usedItem.itemStack.getAmount();

      if (usedItem.useCount >= currentAmount) {
        usedItem.inventory.setItem(usedItem.slotIndex, null);
        continue;
      }

      usedItem.itemStack.setAmount(currentAmount - usedItem.useCount);
    }
  }

  private void tryRecomputeCachedRecipes(ItemStack[] matrixContents) {
    matrixCacheHelper.runIfMatrixChanged(matrixContents, () -> {
      cachedRecipes.clear();

      this.matrixChoices = MatrixChoices.map(matrixContents, similarMaterialsResolver);

      for (var cachedRecipe : recipeCache.getRecipes()) {
        if (!cachedRecipe.areMatrixContentsSatisfyingRecipe(matrixChoices))
          continue;

        cachedRecipes.add(cachedRecipe);
      }
    });
  }

  private boolean tryWalkAttachedDroppersAndPossiblyContainers(
    Block originBlock,
    BlockFace walkingDirection,
    List<Inventory> output
  ) {
    var encounteredDroppers = false;

    for (var offset = 1; offset <= MAX_ATTACHED_CONTAINER_COUNT; ++offset) {
      var nextBlock = originBlock.getRelative(walkingDirection, offset);

      if (!isBlockLoaded(nextBlock))
        break;

      var blockData = nextBlock.getBlockData();

      if (blockData.getMaterial() != Material.DROPPER)
        break;

      encounteredDroppers = true;

      if (!(blockData instanceof Directional directional))
        break;

      var dropperFacing = directional.getFacing();

      if (dropperFacing == walkingDirection.getOppositeFace())
        break;

      var containerBlock = nextBlock.getRelative(dropperFacing);

      if (!BlockUtil.areAllContainerBlocksLoaded(containerBlock, null))
        continue;

      if (!(containerBlock.getState(false) instanceof Container container))
        continue;

      output.add(container.getInventory());
    }

    return encounteredDroppers;
  }
}

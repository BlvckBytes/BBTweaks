package me.blvckbytes.bbtweaks.mechanic.planter;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlanterInstance extends SISOInstance {

  // Finding a random block within the search-area often times yields one which is not
  // even plantable in the first place; let's allow to try multiple times as a
  // mitigation. This also increases the overall planting-speed.
  private static final int MAX_TRIAL_COUNT = 40;

  private final SearchArea searchArea;

  private @Nullable BlockData lastKnownContainerData;

  public PlanterInstance(
    Sign sign,
    Side side,
    int radius
  ) {
    super(sign, side);

    this.searchArea = new SearchArea(getMountBlock().getLocation(), radius);
  }

  @Override
  public boolean tick(long time) {
    plant();
    return true;
  }

  private void plant() {
    var containerBlock = getMountBlock().getRelative(0, 1, 0);

    if (!BlockUtil.areAllContainerBlocksLoaded(containerBlock, lastKnownContainerData))
      return;

    lastKnownContainerData = containerBlock.getBlockData();

    var containerType = lastKnownContainerData.getMaterial();

    // Has a chest attached on top of the block where the sign is mounted - take items from its storage
    if (containerType == Material.CHEST || containerType == Material.TRAPPED_CHEST) {
      if (!(containerBlock.getState(false) instanceof Container container))
        return;

      var containerInventory = container.getInventory();

      var containerSlot = 0;
      var containerSize = containerInventory.getSize();

      var trialIndex = 0;

      while (trialIndex < MAX_TRIAL_COUNT) {
        if (containerSlot >= containerSize)
          break;

        var chestItem = containerInventory.getItem(containerSlot);

        // Current slot is unusable for planting - skip over
        if (!isStackValid(chestItem) || !isItemPlantable(chestItem)) {
          ++containerSlot;
          continue;
        }

        var targetBlock = tryGetBlockToPlantIn(chestItem);

        ++trialIndex;

        if (targetBlock == null)
          continue;

        var plantSuccess = tryPlantItemInBlock(chestItem, targetBlock);

        if (!plantSuccess)
          continue;

        if (chestItem.getAmount() == 1) {
          containerInventory.setItem(containerSlot, null);
          return;
        }

        chestItem.setAmount(chestItem.getAmount() - 1);
        return;
      }
    }

    // Has no container attached - take items from nearby item-entities
    else {
      var areaEntities = searchArea.getEntitiesInArea();

      int entityIndex = 0;

      var trialIndex = 0;

      while (trialIndex < MAX_TRIAL_COUNT) {
        if (entityIndex >= areaEntities.size())
          break;

        var entity = areaEntities.get(entityIndex);

        ItemStack entityStack;

        // Current entity is unusable for planting - skip over
        if (
          !(entity instanceof Item itemEntity)
            || entity.isDead()
            || !entity.isValid()
            || !isStackValid(entityStack = itemEntity.getItemStack())
        ) {
          ++entityIndex;
          continue;
        }

        var targetBlock = tryGetBlockToPlantIn(entityStack);

        ++trialIndex;

        if (targetBlock == null)
          continue;

        // First try to plant, then take the item from the entity - this was a long-standing
        // upstream bug, which made items vanish without being planted on the surrounding field.

        var plantSuccess = tryPlantItemInBlock(entityStack, targetBlock);

        if (!plantSuccess)
          continue;

        if (entityStack.getAmount() == 1) {
          itemEntity.remove();
          return;
        }

        entityStack.setAmount(entityStack.getAmount() - 1);
        itemEntity.setItemStack(entityStack);
        return;
      }
    }
  }

  private @Nullable Block tryGetBlockToPlantIn(ItemStack itemToPlant) {
    var block = searchArea.tryGetRandomBlockInArea();

    if (block == null)
      return null;

    if (!BlockUtil.isBlockLoaded(block))
      return null;

    if (block.getType() != Material.AIR)
      return null;

    if (isItemPlantableInBlock(itemToPlant, block))
      return block;

    return null;
  }

  private boolean isItemPlantable(ItemStack itemToPlant) {
    var itemType = itemToPlant.getType();

    return switch (itemType) {
      case WHEAT_SEEDS, NETHER_WART, MELON_SEEDS, PUMPKIN_SEEDS, CACTUS, POTATO, CARROT, POPPY, DANDELION,
           RED_MUSHROOM, BROWN_MUSHROOM, LILY_PAD, BEETROOT_SEEDS, COCOA_BEANS, CRIMSON_FUNGUS, WARPED_FUNGUS,
           PITCHER_POD, TORCHFLOWER_SEEDS -> true;
      default -> Tag.SAPLINGS.isTagged(itemType);
    };
  }

  private boolean isItemPlantableInBlock(ItemStack itemToPlant, Block blockToPlantIn) {
    var belowType = blockToPlantIn.getRelative(0, -1, 0).getType();

    switch (itemToPlant.getType()) {
      case WHEAT_SEEDS:
      case MELON_SEEDS:
      case PUMPKIN_SEEDS:
      case POTATO:
      case CARROT:
      case BEETROOT_SEEDS:
      case PITCHER_POD:
      case TORCHFLOWER_SEEDS:
        return belowType == Material.FARMLAND;
      case NETHER_WART:
        return belowType == Material.SOUL_SAND;
      case CACTUS:
        return belowType == Material.SAND;
      case RED_MUSHROOM:
      case BROWN_MUSHROOM:
        return belowType.isSolid();
      case LILY_PAD:
        return belowType == Material.WATER;
      case COCOA_BEANS:
        BlockFace[] faces = new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
        for(BlockFace face : faces) {
          if(blockToPlantIn.getRelative(face).getType() == Material.JUNGLE_LOG)
            return true;
        }
        return false;
      case CRIMSON_FUNGUS:
      case WARPED_FUNGUS:
        return belowType == Material.CRIMSON_NYLIUM || belowType == Material.WARPED_NYLIUM || Tag.DIRT.isTagged(belowType);
      default:
        if (itemToPlant.getType() == Material.WITHER_ROSE) {
          if (belowType == Material.SOUL_SOIL || belowType == Material.SOUL_SAND)
            return true;
        }

        if (Tag.SAPLINGS.isTagged(itemToPlant.getType()) || Tag.SMALL_FLOWERS.isTagged(itemToPlant.getType())) {
          return switch (belowType) {
            case MOSS_BLOCK, MUD, MYCELIUM, ROOTED_DIRT, PALE_MOSS_BLOCK,
                 MUDDY_MANGROVE_ROOTS, PODZOL, GRASS_BLOCK, COARSE_DIRT, DIRT -> true;
            default -> false;
          };
        }

        return false;
    }
  }

  private boolean tryPlantItemInBlock(ItemStack itemToPlant, Block blockToPlantIn) {
    var itemType = itemToPlant.getType();

    switch (itemType) {
      case POPPY:
      case DANDELION:
      case CACTUS:
      case RED_MUSHROOM:
      case BROWN_MUSHROOM:
      case LILY_PAD:
        blockToPlantIn.setType(itemType);
        return true;
      case WHEAT_SEEDS:
        blockToPlantIn.setType(Material.WHEAT);
        return true;
      case MELON_SEEDS:
        blockToPlantIn.setType(Material.MELON_STEM);
        return true;
      case PUMPKIN_SEEDS:
        blockToPlantIn.setType(Material.PUMPKIN_STEM);
        return true;
      case NETHER_WART:
        blockToPlantIn.setType(Material.NETHER_WART);
        return true;
      case POTATO:
        blockToPlantIn.setType(Material.POTATOES);
        return true;
      case CARROT:
        blockToPlantIn.setType(Material.CARROTS);
        return true;
      case BEETROOT_SEEDS:
        blockToPlantIn.setType(Material.BEETROOTS);
        return true;
      case COCOA_BEANS:
        List<BlockFace> faces =
          new ArrayList<>(Arrays.asList(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH));
        Collections.shuffle(faces, ThreadLocalRandom.current());
        for(BlockFace face : faces) {
          if(blockToPlantIn.getRelative(face).getType() == Material.JUNGLE_LOG) {
            blockToPlantIn.setType(Material.COCOA);
            ((Cocoa) blockToPlantIn.getBlockData()).setFacing(face);
            return true;
          }
        }
        return false;
      case CRIMSON_FUNGUS:
        blockToPlantIn.setType(Material.CRIMSON_FUNGUS);
        return true;
      case WARPED_FUNGUS:
        blockToPlantIn.setType(Material.WARPED_FUNGUS);
        return true;
      case TORCHFLOWER_SEEDS:
        blockToPlantIn.setType(Material.TORCHFLOWER_CROP);
        return true;
      case PITCHER_POD:
        blockToPlantIn.setType(Material.PITCHER_CROP);
        return true;
      default:
        if (Tag.SAPLINGS.isTagged(itemType)) {
          blockToPlantIn.setType(itemType);
          return true;
        }
        return false;
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isStackValid(ItemStack item) {
    if (item == null)
      return false;

    if (item.getType().isAir())
      return false;

    return item.getAmount() > 0;
  }
}

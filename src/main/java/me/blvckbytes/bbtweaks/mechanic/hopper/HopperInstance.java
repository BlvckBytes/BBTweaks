package me.blvckbytes.bbtweaks.mechanic.hopper;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HopperInstance extends SISOInstance {

  private static final int[] BREWER_POTION_SLOTS = { 0, 1, 2 };

  private @Nullable Inventory hopperInventory;
  private final @Nullable ItemPredicate predicate;
  private final ItemCompatibilities itemCompatibilities;
  private final ConfigKeeper<MainSection> config;

  public HopperInstance(
    Sign sign,
    @Nullable ItemPredicate predicate,
    ItemCompatibilities itemCompatibilities,
    ConfigKeeper<MainSection> config
  ) {
    super(sign);

    this.predicate = predicate;
    this.itemCompatibilities = itemCompatibilities;
    this.config = config;
  }

  public Block getMountBlock() {
    return mountBlock;
  }

  @Override
  public boolean tick(int time) {
    if (time % config.rootSection.mechanic.hopper.transportCycleTicks != 0)
      return true;

    var inputPower = tryReadInputPower();

    if (inputPower == null || inputPower > 0)
      return true;

    if (!isBlockLoaded(mountBlock)) {
      hopperInventory = null;
      return true;
    }

    if (hopperInventory == null)
      hopperInventory = ((InventoryHolder) mountBlock.getState()).getInventory();

    var mountBlockData = mountBlock.getBlockData();

    if (mountBlockData.getMaterial() != Material.HOPPER)
      return false;

    var hopperData = (Hopper) mountBlockData;

    // Disable the hopper, since we cannot work with "vanilla" move-events in
    // any case, and they would waste needless performance over time.
    if (hopperData.isEnabled()) {
      hopperData.setEnabled(false);
      mountBlock.setBlockData(hopperData);
    }

    // Prefer the hopper's own internal inventory and only fall back to the source-block
    // if it has no contents to be moved (empty or excluded by the configured predicate).
    var sourceSlot = getSlotOfItemToTransport(hopperInventory);
    var sourceInventory = hopperInventory;

    if (sourceSlot < 0) {
      var sourceBlock = mountBlock.getRelative(BlockFace.UP);

      if (!isBlockLoaded(sourceBlock))
        return true;

      if (!(sourceBlock.getState() instanceof InventoryHolder sourceInventoryHolder))
        return true;

      sourceInventory = sourceInventoryHolder.getInventory();
      sourceSlot = getSlotOfItemToTransport(sourceInventory);

      if (sourceSlot < 0)
        return true;
    }

    var hopperFacing = hopperData.getFacing();
    var destinationBlock = mountBlock.getRelative(hopperFacing);

    if (!isBlockLoaded(destinationBlock))
      return true;

    if (!(destinationBlock.getState() instanceof InventoryHolder destinationInventoryHolder))
      return true;

    var sourceItem = sourceInventory.getItem(sourceSlot);

    if (sourceItem == null)
      return true;

    var remainingAmount = tryTransportItemAndGetRemainder(destinationInventoryHolder.getInventory(), destinationBlock.getType(), sourceItem, hopperFacing);

    if (remainingAmount == sourceItem.getAmount())
      return true;

    if (remainingAmount <= 0) {
      sourceInventory.setItem(sourceSlot, null);
      return true;
    }

    sourceItem.setAmount(remainingAmount);
    return true;
  }

  private int tryTransportItemAndGetRemainder(Inventory destinationInventory, Material destinationBlockType, ItemStack item, BlockFace hopperFacing) {
    if (destinationInventory instanceof BrewerInventory) {
      if (hopperFacing == BlockFace.DOWN) {
        if (itemCompatibilities.isBrewingIngredient(item)) {
          var currentItem = destinationInventory.getItem(3);

          if (isAir(currentItem)) {
            destinationInventory.setItem(3, item);
            return 0;
          }

          return tryStackAndGetRemainder(item, currentItem);
        }

        return item.getAmount();
      }

      if (itemCompatibilities.isBrewingFuel(item)) {
        var currentItem = destinationInventory.getItem(4);

        if (isAir(currentItem)) {
          destinationInventory.setItem(4, item);
          return 0;
        }

        return tryStackAndGetRemainder(item, currentItem);
      }

      if (itemCompatibilities.isPotion(item)) {
        for (var slotIndex : BREWER_POTION_SLOTS) {
          var currentItem = destinationInventory.getItem(slotIndex);

          if (!isAir(currentItem))
            continue;

          var itemAmount = item.getAmount();

          if (itemAmount <= 1) {
            if (itemAmount > 0)
              destinationInventory.setItem(slotIndex, item);

            return 0;
          }

          var clone = new ItemStack(item);
          clone.setAmount(1);

          destinationInventory.setItem(slotIndex, clone);
          item.setAmount(itemAmount - 1);
        }

        return item.getAmount();
      }

      return item.getAmount();
    }

    if (destinationInventory instanceof FurnaceInventory) {
      var furnaceType = FurnaceType.fromBlockMaterial(destinationBlockType);

      if (furnaceType == null)
        return item.getAmount();

      if (hopperFacing == BlockFace.DOWN) {
        if (itemCompatibilities.isFurnaceIngredient(furnaceType, item)) {
          var currentItem = destinationInventory.getItem(0);

          if (isAir(currentItem)) {
            destinationInventory.setItem(0, item);
            return 0;
          }

          return tryStackAndGetRemainder(item, currentItem);
        }

        return item.getAmount();
      }

      if (itemCompatibilities.isFurnaceFuel(furnaceType, item)) {
        var currentItem = destinationInventory.getItem(1);

        if (isAir(currentItem)) {
          destinationInventory.setItem(1, item);
          return 0;
        }

        return tryStackAndGetRemainder(item, currentItem);
      }

      return item.getAmount();
    }

    for (var slotIndex = 0; slotIndex < destinationInventory.getSize(); ++slotIndex) {
      var currentItem = destinationInventory.getItem(slotIndex);

      if (isAir(currentItem)) {
        destinationInventory.setItem(slotIndex, item);
        return 0;
      }

      var remainder = tryStackAndGetRemainder(item, currentItem);

      if (remainder <= 0)
        return 0;

      item.setAmount(remainder);
    }

    return item.getAmount();
  }

  private int tryStackAndGetRemainder(@NotNull ItemStack source, @NotNull ItemStack destination) {
    if (!(source.isSimilar(destination)))
      return source.getAmount();

    var destinationSpace = Math.max(0, destination.getMaxStackSize() - destination.getAmount());
    var stackableCount = Math.min(source.getAmount(), destinationSpace);

    if (stackableCount <= 0)
      return source.getAmount();

    destination.setAmount(destination.getAmount() + stackableCount);

    return source.getAmount() - stackableCount;
  }

  private int getSlotOfItemToTransport(Inventory inventory) {
    if (inventory instanceof BrewerInventory) {
      for (var slotIndex : BREWER_POTION_SLOTS) {
        if (shouldTransportItem(inventory.getItem(slotIndex)))
          return slotIndex;
      }

      return -1;
    }

    if (inventory instanceof FurnaceInventory) {
      if (shouldTransportItem(inventory.getItem(2)))
        return 2;

      return -1;
    }

    for (var slotIndex = 0; slotIndex < inventory.getSize(); ++slotIndex) {
      if (shouldTransportItem(inventory.getItem(slotIndex)))
        return slotIndex;
    }

    return -1;
  }

  private boolean isAir(@Nullable ItemStack item) {
    return item == null || item.getType().isAir() || item.getAmount() <= 0;
  }

  private boolean shouldTransportItem(@Nullable ItemStack item) {
    if (isAir(item))
      return false;

    return predicate == null || predicate.test(item);
  }
}

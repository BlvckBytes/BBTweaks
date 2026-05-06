package me.blvckbytes.bbtweaks.shulker_accessor;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.shulker_accessor.change_detection.ChangeDetectionHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShulkerAccessorHolder extends ChangeDetectionHolder {

  private record BlockAndData(Block block, BlockData data) {
    BlockAndData(Block block) {
      this(block, block.getBlockData());
    }
  }

  private final ItemStack shulkerItem;
  private final Inventory itemContainingInventory;
  private final int itemContainingInventorySlot;
  private final List<BlockAndData> containerBlockCaptures;

  private ShulkerAccessorHolder(
    ItemStack shulkerItem,
    Inventory itemContainingInventory,
    int itemContainingInventorySlot,
    List<BlockAndData> containerBlockCaptures
  ) {
    this.shulkerItem = shulkerItem;
    this.itemContainingInventory = itemContainingInventory;
    this.itemContainingInventorySlot = itemContainingInventorySlot;
    this.containerBlockCaptures = containerBlockCaptures;
  }

  @Override
  public boolean isValid() {
    for (var blockAndData : containerBlockCaptures) {
      var world = blockAndData.block.getWorld();

      if (!world.isChunkLoaded(blockAndData.block.getX() >> 4, blockAndData.block.getZ() >> 4))
        return false;

      if (!blockAndData.data.equals(blockAndData.block.getBlockData()))
        return false;
    }

    var currentItem = itemContainingInventory.getItem(itemContainingInventorySlot);

    if (currentItem == null || currentItem.getType().isAir())
      return false;

    return shulkerItem.equals(currentItem);
  }

  public boolean sharesBlocksAndSlotWith(ShulkerAccessorHolder other) {
    for (var thisBlockCapture : containerBlockCaptures) {
      for (var otherBlockCapture : other.containerBlockCaptures) {
        if (areBlockPositionsEqual(thisBlockCapture.block, otherBlockCapture.block))
          return this.itemContainingInventorySlot == other.itemContainingInventorySlot;
      }
    }

    return false;
  }

  private boolean areBlockPositionsEqual(Block a, Block b) {
    if (a.getWorld() != b.getWorld())
      return false;

    return a.getX() == b.getX() && a.getY() == b.getY() && a.getZ() == b.getZ();
  }

  public boolean isShulkerItemContainedByInventoryAtSlot(Inventory inventory, int slot) {
    if (itemContainingInventory != inventory)
      return false;

    return itemContainingInventorySlot == slot;
  }

  public boolean tryWriteBackToItem() {
    if (shulkerItem.getAmount() != 1 || !Tag.SHULKER_BOXES.isTagged(shulkerItem.getType()))
      return false;

    if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta))
      return false;

    if (!(blockStateMeta.getBlockState() instanceof Container container))
      return false;

    container.getInventory().setContents(getInventory().getContents());

    blockStateMeta.setBlockState(container);
    shulkerItem.setItemMeta(blockStateMeta);

    return true;
  }

  public static @Nullable ShulkerAccessorHolder instantiateIfValidShulker(
    ItemStack item,
    Inventory itemContainingInventory,
    int itemContainingInventorySlot,
    @Nullable Component currentViewTitle,
    ConfigKeeper<MainSection> config
  ) {
    if (item.getAmount() != 1 || !Tag.SHULKER_BOXES.isTagged(item.getType()))
      return null;

    var containerBlockCaptures = getContainerBlocksOrNullIfInvalid(itemContainingInventory, currentViewTitle, config);

    if (containerBlockCaptures == null)
      return null;

    if (!(item.getItemMeta() instanceof BlockStateMeta blockStateMeta))
      return null;

    if (!(blockStateMeta.getBlockState() instanceof Container container))
      return null;

    var shulkerInventory = container.getInventory();

    var holder = new ShulkerAccessorHolder(item, itemContainingInventory, itemContainingInventorySlot, containerBlockCaptures);

    Inventory inventory;

    var itemName = blockStateMeta.displayName();

    if (itemName != null)
      inventory = Bukkit.createInventory(holder, InventoryType.SHULKER_BOX, itemName);
    else
      inventory = Bukkit.createInventory(holder, InventoryType.SHULKER_BOX);

    inventory.setContents(shulkerInventory.getContents());

    holder.setInventory(inventory);

    return holder;
  }

  private static @Nullable List<BlockAndData> getContainerBlocksOrNullIfInvalid(
    Inventory itemContainingInventory,
    @Nullable Component currentViewTitle,
    ConfigKeeper<MainSection> config
  ) {
    if (itemContainingInventory instanceof PlayerInventory)
      return Collections.emptyList();

    if (itemContainingInventory.getType() == InventoryType.ENDER_CHEST)
      return Collections.emptyList();

    if (itemContainingInventory instanceof DoubleChestInventory doubleChestInventory) {
      var containerBlocks = new ArrayList<BlockAndData>();

      if (doubleChestInventory.getRightSide().getHolder() instanceof Container rightContainer)
        containerBlocks.add(new BlockAndData(rightContainer.getBlock()));

      if (doubleChestInventory.getLeftSide().getHolder() instanceof Container leftContainer)
        containerBlocks.add(new BlockAndData(leftContainer.getBlock()));

      if (containerBlocks.isEmpty())
        return null;

      return containerBlocks;
    }

    if (itemContainingInventory.getHolder() instanceof Container container)
      return Collections.singletonList(new BlockAndData(container.getBlock()));

    if (currentViewTitle == null)
      return null;

    var titleTextBuilder = new StringBuilder();

    walkTextComponentsAndAppend(currentViewTitle, titleTextBuilder);

    var titleText = titleTextBuilder.toString();

    if (titleText.isBlank())
      return null;

    for (var pattern : config.rootSection.shulkerAccessor._additionalAllowedInventoryTitlePatterns) {
      if (pattern.matcher(titleText).matches())
        return Collections.emptyList();
    }

    return null;
  }

  private static void walkTextComponentsAndAppend(Component component, StringBuilder output) {
    if (component instanceof TextComponent textComponent)
      output.append(textComponent.content());

    for (var child : component.children())
      walkTextComponentsAndAppend(child, output);
  }
}

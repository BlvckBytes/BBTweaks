package me.blvckbytes.bbtweaks.pipes.search.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.container_ticket.PreRemoteContainerOpenEvent;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.bbtweaks.pipes.search.*;
import me.blvckbytes.bbtweaks.util.BlockUtil;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class PipeSearchDisplayHandler extends DisplayHandler<PipeSearchDisplay, SearchDisplayData> {

  private static final BlockFace[] DIRECT_FACES = {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
    BlockFace.UP, BlockFace.DOWN
  };

  private final Logger logger;

  private final FloodgateIntegration floodgateIntegration;

  public PipeSearchDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    FloodgateIntegration floodgateIntegration
  ) {
    super(config, plugin, PipeSearchDisplay.class);

    this.logger = plugin.getLogger();

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected PipeSearchDisplay instantiateDisplay(Player player, SearchDisplayData displayData) {
    return new PipeSearchDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  private void handleStackAction(Player player, PipeSearchDisplay display, StackAction stackAction, ItemStackEntry itemEntry) {
    if (stackAction == StackAction.TELEPORT_TO_CONTAINER) {
      if (teleportPlayerToContainer(player, itemEntry.itemAndSlot.block(), config))
        player.closeInventory();
      return;
    }

    if (stackAction == StackAction.MOVE_TO_INVENTORY) {
      var moveResultAndAmount = moveItemIntoInventory(player, itemEntry.itemAndSlot, Integer.MAX_VALUE);
      var moveResult = moveResultAndAmount.moveResult();

      if (moveResult == MoveResult.NO_SPACE) {
        // TODO: Add requested item-type to this message
        config.rootSection.pipes.search.getItemNoSpace.sendMessage(player);
        return;
      }

      if (moveResult == MoveResult.DID_MOVE || moveResult == MoveResult.INVALID_ITEM)
        display.removeEntry(itemEntry);

      if (moveResult == MoveResult.DID_DECREMENT)
        display.updateItems();

      var movedAmount = moveResultAndAmount.movedAmount();

      if (movedAmount > 0)
        sendHandOutMessage(player, movedAmount, itemEntry.itemAndSlot.stackSize(), itemEntry.itemAndSlot.type());

      return;
    }

    if (stackAction == StackAction.OPEN_CONTAINER) {
      if (!openContainer(player, itemEntry.itemAndSlot))
        display.removeEntry(itemEntry);
    }
  }

  private void handleStackClick(Player player, PipeSearchDisplay display, ClickType clickType, ItemStackEntry itemEntry) {
    if (display.isFloodgate) {
      if (clickType == ClickType.LEFT) {
        handleStackAction(player, display, display.getStackAction(), itemEntry);
        return;
      }

      if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
        display.nextStackAction();
        return;
      }

      return;
    }

    if (clickType == ClickType.LEFT) {
      handleStackAction(player, display, StackAction.TELEPORT_TO_CONTAINER, itemEntry);
      return;
    }

    if (clickType == ClickType.DROP) {
      handleStackAction(player, display, StackAction.MOVE_TO_INVENTORY, itemEntry);
      return;
    }

    if (clickType == ClickType.CONTROL_DROP)
      handleStackAction(player, display, StackAction.OPEN_CONTAINER, itemEntry);
  }

  private void handleCollectionClick(Player player, PipeSearchDisplay display, ClickType clickType, ItemCollectionEntry collectionEntry) {
    if (display.isFloodgate) {
      if (clickType == ClickType.LEFT) {
        handleCollectionAction(player, display, display.getCollectionAction(), collectionEntry);
        return;
      }

      if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
        display.nextCollectionAction();
        return;
      }

      return;
    }

    if (clickType == ClickType.LEFT) {
      handleCollectionAction(player, display, CollectionAction.SHOW_STACKS, collectionEntry);
      return;
    }

    if (clickType == ClickType.DROP) {
      handleCollectionAction(player, display, CollectionAction.GET_ONE_STACK, collectionEntry);
      return;
    }

    if (clickType == ClickType.CONTROL_DROP) {
      handleCollectionAction(player, display, CollectionAction.FILL_INVENTORY, collectionEntry);
      return;
    }

    if (clickType == ClickType.RIGHT)
      handleCollectionAction(player, display, CollectionAction.GET_FOUR_STACKS, collectionEntry);
  }

  private void handleCollectionAction(Player player, PipeSearchDisplay display, CollectionAction action, ItemCollectionEntry collectionEntry) {
    if (action == CollectionAction.SHOW_STACKS) {
      show(player, new SearchDisplayData(display.displayData.predicate(), collectionEntry.getMembersAsEntries(), display));
      return;
    }

    if (action == CollectionAction.GET_ONE_STACK) {
      handleMovingItems(player, display, collectionEntry, collectionEntry.getStackSize());
      return;
    }

    if (action == CollectionAction.FILL_INVENTORY) {
      handleMovingItems(player, display, collectionEntry, Integer.MAX_VALUE);
      return;
    }

    if (action == CollectionAction.GET_FOUR_STACKS)
      handleMovingItems(player, display, collectionEntry, collectionEntry.getStackSize() * 4);
  }

  private void sendHandOutMessage(Player player, int totalHandOutAmount, int stackSize, Material type) {
    var numberStacks = totalHandOutAmount / stackSize;
    var singleItems = totalHandOutAmount % stackSize;
    var numberDoubleChests = (double) numberStacks / (6 * 9);

    config.rootSection.pipes.search.getItemSuccess.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("number_stacks", numberStacks)
        .withVariable("number_double_chests", numberDoubleChests)
        .withVariable("stack_size", stackSize)
        .withVariable("single_items", singleItems)
        .withVariable("item_type_key", type.translationKey())
    );
  }

  public void handleMovingItems(Player player, @Nullable PipeSearchDisplay display, ItemCollectionEntry collectionEntry, int maximumAmount) {
    var totalHandOutAmount = 0;
    var ranOutOfSpace = false;

    ItemAndSlot nextMember;

    while ((nextMember = collectionEntry.getFirstMember()) != null) {
      // TODO: This really should be silent and only send a single warning at the end of the stack-action if we could
      //       not fulfill the request - the user doesn't care if two out of 20 stacks moved, as long as they still
      //       get what they need.
      var moveResultAndAmount = moveItemIntoInventory(player, nextMember, maximumAmount - totalHandOutAmount);
      var moveResult = moveResultAndAmount.moveResult();

      if (moveResult == MoveResult.NO_SPACE) {
        ranOutOfSpace = true;
        break;
      }

      totalHandOutAmount += moveResultAndAmount.movedAmount();

      if (moveResult == MoveResult.INVALID_ITEM || moveResult == MoveResult.DID_MOVE)
        collectionEntry.removeMember(nextMember);

      if (totalHandOutAmount >= maximumAmount)
        break;
    }

    if (totalHandOutAmount > 0)
      sendHandOutMessage(player, totalHandOutAmount, collectionEntry.getStackSize(), collectionEntry.getMaterial());

    // TODO: Add requested item-type to this message
    else if (ranOutOfSpace)
      config.rootSection.pipes.search.getItemNoSpace.sendMessage(player);

    // Exhausted the collection - make it vanish altogether
    if (collectionEntry.isEmpty()) {
      if (display != null)
        display.removeEntry(collectionEntry);

      return;
    }

    // Synchronize the displayed counts
    if (totalHandOutAmount > 0 && display != null)
      display.updateItems();
  }

  @Override
  protected void handleClick(Player player, PipeSearchDisplay display, ClickType clickType, int slot) {
    var targetEntry = display.getEntryCorrespondingToSlot(slot);

    if (targetEntry != null) {
      if (targetEntry instanceof ItemStackEntry itemStackEntry) {
        handleStackClick(player, display, clickType, itemStackEntry);
        return;
      }

      if (targetEntry instanceof ItemCollectionEntry collectionEntry) {
        handleCollectionClick(player, display, clickType, collectionEntry);
        return;
      }

      logger.warning("Encountered unaccounted-for result-display entry-type: " + targetEntry.getClass());
      return;
    }

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.pipes.search.display.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.pipes.search.display.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (display.displayData.backToDisplay() != null && config.rootSection.pipes.search.display.items.backToCollectionsButton.getDisplaySlots().contains(slot)) {
        display.displayData.backToDisplay().showNextTick();
        return;
      }
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.pipes.search.display.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.pipes.search.display.items.nextPage.getDisplaySlots().contains(slot))
        display.lastPage();
    }
  }

  public static boolean teleportPlayerToContainer(Player player, Block block, ConfigKeeper<MainSection> config) {
    var destinationBlock = block;
    var targetContainer = block;

    if (block.getBlockData() instanceof Directional directional) {
      var facing = directional.getFacing();
      destinationBlock = block.getRelative(facing.getModX(), facing.getModY(), facing.getModZ());

      // [1] Move one more away in this direction if possible, to allow for some breathing-space.
      if (destinationBlock.isPassable())
        destinationBlock = destinationBlock.getRelative(facing.getModX(), facing.getModY(), facing.getModZ());
    }

    if (!destinationBlock.isPassable() && block.getState() instanceof Container container) {
      var blocks = new Block[]{ block, null };

      if (container.getInventory() instanceof DoubleChestInventory doubleInventory) {
        if (doubleInventory.getRightSide().getHolder(false) instanceof Container rightContainer)
          blocks[0] = rightContainer.getBlock();

        if (doubleInventory.getLeftSide().getHolder(false) instanceof Container leftContainer)
          blocks[1] = leftContainer.getBlock();
      }

      blockLoop: for (var currentBlock : blocks) {
        if (currentBlock == null)
          continue;

        for (var nextFacing : DIRECT_FACES) {
          var nextDestinationBlock = currentBlock.getRelative(nextFacing.getModX(), nextFacing.getModY(), nextFacing.getModZ());

          if (!nextDestinationBlock.isPassable())
            continue;

          // Ensure that the player looks at the closest container to them, in case of a double-chest
          // with the short-side pointing outwards to where they'll be teleported.
          targetContainer = currentBlock;

          // Same as [1]
          var oneMoreApart = nextDestinationBlock.getRelative(nextFacing.getModX(), nextFacing.getModY(), nextFacing.getModZ());

          if (oneMoreApart.isPassable())
            destinationBlock = oneMoreApart;
          else
            destinationBlock = nextDestinationBlock;

          break blockLoop;
        }
      }
    }

    var environment = getBlockEnvironment(block);

    if (!destinationBlock.isPassable()) {
      config.rootSection.pipes.search.containerTeleportObstructed.sendMessage(player, environment);
      return false;
    }

    var lookedAtCenter = targetContainer.getLocation().add(.5, .5, .5);

    // Center up on the destination-block; otherwise, the player will be partially
    // stuck in a block in tight spaces.
    var footLocation = destinationBlock.getLocation().add(.5, 0, .5);

    var eyeLocation = footLocation.clone().add(0, 1.6, 0);
    var direction = lookedAtCenter.toVector().subtract(eyeLocation.toVector()).normalize();
    footLocation.setDirection(direction);

    player.teleport(footLocation);

    config.rootSection.pipes.search.containerTeleported.sendMessage(player, environment);
    return true;
  }

  private MoveResultAndAmount moveItemIntoInventory(Player player, ItemAndSlot item, int maximumAmount) {
    var containerBlock = item.block();
    var containerInventory = BlockUtil.tryAccessBlockInventory(containerBlock);

    var environment = getBlockEnvironment(containerBlock);

    if (containerInventory == null) {
      config.rootSection.pipes.search.getItemContainerAbsent.sendMessage(player, environment);
      return new MoveResultAndAmount(MoveResult.INVALID_ITEM, 0);
    }

    if (item.slot() < 0 || item.slot() >= containerInventory.getSize()) {
      config.rootSection.pipes.search.getItemContainerSizeChanged.sendMessage(player, environment);
      return new MoveResultAndAmount(MoveResult.INVALID_ITEM, 0);
    }

    var targetItem = containerInventory.getItem(item.slot());

    environment
      .withVariable("item_slot", item.slot() + 1)
      .withVariable("item_amount", item.item().getAmount())
      .withVariable("item_type_key", item.type().translationKey());

    if (!item.item().equals(targetItem)) {
      config.rootSection.pipes.search.getItemMoved.sendMessage(player, environment);
      return new MoveResultAndAmount(MoveResult.INVALID_ITEM, 0);
    }

    var amountToAdd = Math.min(targetItem.getAmount(), maximumAmount);
    var amountNotAdded = InventoryUtil.addItemToInventoryAndGetRemainingAmount(targetItem, amountToAdd, player.getInventory());
    var addedAmount = amountToAdd - amountNotAdded;
    var remainingAmount = targetItem.getAmount() - addedAmount;

    if (remainingAmount <= 0) {
      containerInventory.setItem(item.slot(), null);
      return new MoveResultAndAmount(MoveResult.DID_MOVE, addedAmount);
    }

    if (amountNotAdded >= amountToAdd)
      return new MoveResultAndAmount(MoveResult.NO_SPACE, 0);

    targetItem.setAmount(remainingAmount);

    return new MoveResultAndAmount(MoveResult.DID_DECREMENT, addedAmount);
  }

  private boolean openContainer(Player player, ItemAndSlot item) {
    var containerBlock = item.block();
    var containerInventory = BlockUtil.tryAccessBlockInventory(containerBlock);

    var environment = getBlockEnvironment(containerBlock);

    if (containerInventory == null) {
      config.rootSection.pipes.search.getItemContainerAbsent.sendMessage(player, environment);
      return false;
    }

    Bukkit.getPluginManager().callEvent(new PreRemoteContainerOpenEvent(containerBlock, containerInventory));
    player.openInventory(containerInventory);

    config.rootSection.pipes.search.containerOpened.sendMessage(player, environment);
    return true;
  }

  public static InterpretationEnvironment getBlockEnvironment(Block block) {
    return new InterpretationEnvironment()
      .withVariable("container_x", block.getX())
      .withVariable("container_y", block.getY())
      .withVariable("container_z", block.getZ());
  }
}

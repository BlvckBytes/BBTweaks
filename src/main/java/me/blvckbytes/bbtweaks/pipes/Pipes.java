package me.blvckbytes.bbtweaks.pipes;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.pipes.notification.*;
import me.blvckbytes.bbtweaks.util.CompactId;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class Pipes implements PipesApi, Listener {

  public static final String PIPE_MARKER = "[Pipe]";

  private static final BlockFace[] DROP_ITEM_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
    BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST,
    BlockFace.UP, BlockFace.DOWN,
  };

  private static final BlockFace[] PIPE_NEIGHBOR_FACES = new BlockFace[] {
    BlockFace.UP, BlockFace.DOWN,
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private static final int FURNACE_RESULT_INDEX = 2;

  private int currentTubeBlockCounter;
  private int currentPistonBlockCounter;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final PipeBlockCacheRegistry cacheRegistry;

  private PipeBlockCache currentBlockCache;

  private final PipeTimingsCommand pipeTimingsCommand;
  private final PipesInventoryUtil inventoryUtil;

  private final Map<UUID, Map<String, Long>> lastNotificationSendByDebounceIdByPlayerId;

  public Pipes(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    PipeBlockCacheRegistry cacheRegistry,
    PipeTimingsCommand pipeTimingsCommand,
    PipesInventoryUtil inventoryUtil
  ) {
    this.plugin = plugin;
    this.config = config;
    this.cacheRegistry = cacheRegistry;
    this.pipeTimingsCommand = pipeTimingsCommand;
    this.inventoryUtil = inventoryUtil;

    this.lastNotificationSendByDebounceIdByPlayerId = new HashMap<>();
  }

  @EventHandler(ignoreCancelled = true)
  public void onSignChange(SignChangeEvent event) {
    if (!ComponentUtil.asTrimmedText(event.line(1)).equalsIgnoreCase(PIPE_MARKER))
      return;

    var player = event.getPlayer();

    if (!player.hasPermission("bbtweaks.pipes")) {
      cancelAndBreakSign(event);
      config.rootSection.pipes.signCreateNoPermission.sendMessage(player);
      return;
    }

    var signBlock = event.getBlock();
    var blockData = signBlock.getBlockData();

    if (blockData instanceof WallSign wallSign) {
      var mountBlock = signBlock.getRelative(wallSign.getFacing().getOppositeFace());

      if (mountBlock.getType() != Material.PISTON && mountBlock.getType() != Material.STICKY_PISTON) {
        config.rootSection.pipes.signCreateNoPistonFound.sendMessage(player);
        cancelAndBreakSign(event);
        return;
      }
    }

    else if (Tag.STANDING_SIGNS.isTagged(blockData.getMaterial())) {
      var blockAbove = signBlock.getRelative(BlockFace.UP);
      var blockBelow = signBlock.getRelative(BlockFace.DOWN);

      if (blockAbove.getType() != Material.PISTON && blockAbove.getType() != Material.STICKY_PISTON) {
        if (blockBelow.getType() != Material.PISTON && blockBelow.getType() != Material.STICKY_PISTON) {
          config.rootSection.pipes.signCreateNoPistonFound.sendMessage(player);
          cancelAndBreakSign(event);
          return;
        }
      }
    }

    else {
      cancelAndBreakSign(event);
      config.rootSection.pipes.unsupportedSignType.sendMessage(player);
      return;
    }

    event.line(1, Component.text(PIPE_MARKER));
    config.rootSection.pipes.signCreated.sendMessage(player);
  }

  private void cancelAndBreakSign(SignChangeEvent event) {
    event.setCancelled(true);
    Bukkit.getScheduler().runTaskLater(plugin, () -> event.getBlock().breakNaturally(), 1);
  }

  private EnumerationResult locateExitNodesForItems(Block inputPistonBlock, LongSet visitedBlocks, EnumSet<LocateFlag> flags, PipeItems pipeItems, List<PipeNotification> notificationOutput) {
    var enumerationFlags = EnumSet.of(EnumerationBehavior.DO_NOT_RESET_CACHE_AND_MAX_COUNTERS);

    // Only reset the limit-counters once, at the very top of the call-stack, seeing
    // how they do apply to the pipe as a whole, including sub-pipes.
    if (flags.remove(LocateFlag.RESET_COUNTERS))
      enumerationFlags.remove(EnumerationBehavior.DO_NOT_RESET_CACHE_AND_MAX_COUNTERS);

    return enumeratePipeBlocks(inputPistonBlock, visitedBlocks, enumerationFlags, (pipeBlock, cachedPipeBlock, _) -> {
      if (pipeItems.isEmptyOrNoneActive())
        return EnumerationDecision.STOP;

      if (!CachedBlock.isMaterial(cachedPipeBlock, Material.PISTON))
        return EnumerationDecision.CONTINUE;

      var putBlock = pipeBlock.getRelative(CachedBlock.getFacing(cachedPipeBlock));
      var putBlockId = CompactId.computeWorldlessBlockId(putBlock);
      var cachedPutBlock = currentBlockCache.getCachedBlock(putBlock);

      // Skip if the put-block is either part of the input-container, whose IDs we added when starting the pipe,
      // or any other glass-blocks that we may have walked across in the past already.
      if (visitedBlocks.contains(putBlockId))
        return EnumerationDecision.CONTINUE;

      var isSubPipe = CachedBlock.isTube(cachedPutBlock) && !CachedBlock.isPane(cachedPutBlock);

      // Add the sub-pipe tube-block to the visited-set as to avoid it being walked into again
      // by the current enumerator on the next iteration, which would render filters useless.
      if (isSubPipe)
        visitedBlocks.add(putBlockId);

      var sign = currentBlockCache.getSignOnPiston(pipeBlock, cachedPipeBlock, notificationOutput);

      if (config.rootSection.pipes.requireSign) {
        if (sign != PipeSign.NO_SIGN)
          flags.add(LocateFlag.ENCOUNTERED_SIGN);

        // Do walk into sub-pipes without requiring a sign
        if (!isSubPipe && !flags.contains(LocateFlag.ENCOUNTERED_SIGN))
          return EnumerationDecision.CONTINUE;
      }

      if (CachedBlock.isMaterial(cachedPutBlock, Material.AIR) || CachedBlock.isMaterial(cachedPutBlock, Material.VOID_AIR))
        return EnumerationDecision.CONTINUE;

      var predicateEvent = new PipePredicateEvent(pipeBlock, sign.includeFilters, sign.excludeFilters);
      Bukkit.getPluginManager().callEvent(predicateEvent);

      var filteredPipeItems = pipeItems.filterAndMakeSub(predicateEvent::testItem);

      if (filteredPipeItems.isEmptyOrNoneActive())
        return EnumerationDecision.CONTINUE;

      if (isSubPipe) {
        if (locateExitNodesForItems(putBlock, visitedBlocks, flags, filteredPipeItems, notificationOutput) != EnumerationResult.COMPLETED)
          return EnumerationDecision.STOP;

        return EnumerationDecision.CONTINUE;
      }

      if (CachedBlock.isPowerable(cachedPutBlock)) {
        currentBlockCache.temporarilyPowerBlock(putBlock, 10);
        return EnumerationDecision.CONTINUE;
      }

      var blockInventory = currentBlockCache.tryAccessPossiblyUnloadedBlockInventory(putBlock, cachedPutBlock);

      if (blockInventory == null)
        return EnumerationDecision.CONTINUE;

      var blockType = CachedBlock.getMaterial(cachedPutBlock);

      filteredPipeItems.forEachActiveItemAndBreakAfterReduce((_, item) -> {
        return inventoryUtil.addItemToInventoryAndGetRemainingAmount(blockInventory, blockType, item);
      });

      return EnumerationDecision.CONTINUE;
    });
  }

  @Override
  public EnumerationResult enumeratePipeBlocks(Block firstBlock, @Nullable LongSet visitedBlocks, EnumSet<EnumerationBehavior> behaviorFlags, PipeEnumerationHandler enumerationHandler) {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("This method must be called on the main server thread");

    try {
      // Seeing how this is public API, assign the current block-cache again, because it
      // will only be correctly set when called through #startPipe.
      this.currentBlockCache = cacheRegistry.getBlockCache(firstBlock.getWorld());

      if (visitedBlocks == null)
        visitedBlocks = new LongOpenHashSet();

      if (!behaviorFlags.contains(EnumerationBehavior.DO_NOT_RESET_CACHE_AND_MAX_COUNTERS)) {
        currentTubeBlockCounter = currentPistonBlockCounter = 0;
        currentBlockCache.resetCacheLoadCounter();
      }

      var searchQueue = new ArrayDeque<Block>();
      searchQueue.addFirst(firstBlock);
      visitedBlocks.add(CompactId.computeWorldlessBlockId(firstBlock));

      var pistonQueue = new ArrayDeque<Block>();
      boolean hasPistons;

      while ((hasPistons = !pistonQueue.isEmpty()) || !searchQueue.isEmpty()) {
        var pipeBlock = hasPistons ? pistonQueue.poll() : searchQueue.poll();
        var cachedPipeBlock = currentBlockCache.getCachedBlock(pipeBlock);

        if (CachedBlock.isTube(cachedPipeBlock))
          ++currentTubeBlockCounter;

        if (CachedBlock.isMaterial(cachedPipeBlock, Material.PISTON)) {
          ++currentPistonBlockCounter;

          if (behaviorFlags.contains(EnumerationBehavior.LOAD_PISTON_SIGNS))
            currentBlockCache.getSignOnPiston(pipeBlock, cachedPipeBlock, null);
        }

        var handleResult = enumerationHandler.handle(pipeBlock, cachedPipeBlock, currentBlockCache);

        if (handleResult != EnumerationDecision.CONTINUE)
          return EnumerationResult.COMPLETED;

        // While we could check for exceeding the load-counter at countless call-sites, and while there already have been
        // a few cache-lookups prior to enumerating, a hand-full blocks more don't matter in the grand scheme of things.
        if (getMaxCacheLoadCount() > 0 && currentBlockCache.getCacheLoadCounter() >= getMaxCacheLoadCount())
          return EnumerationResult.EXCEEDED_CACHE_LOAD_LIMIT;

        for (var neighborFace : PIPE_NEIGHBOR_FACES) {
          var enumeratedBlock = pipeBlock.getRelative(neighborFace);
          var cachedEnumeratedBlock = currentBlockCache.getCachedBlock(enumeratedBlock);

          if (!CachedBlock.isValidPipeBlock(cachedEnumeratedBlock))
            continue;

          // Ensure that the block we came from is of the same color as the one we're enumerating.
          // [1]: Do this first, as to not mark blocks as visited that have not been walked into.
          //      This could become a problem if that other-colored block is a part of the future path.
          if (CachedBlock.doTubeColorsMismatch(cachedPipeBlock, cachedEnumeratedBlock))
            continue;

          if (!visitedBlocks.add(CompactId.computeWorldlessBlockId(enumeratedBlock)))
            continue;

          if (!CachedBlock.isTube(cachedEnumeratedBlock)) {
            if (!CachedBlock.isMaterial(cachedEnumeratedBlock, Material.PISTON))
              continue;

            if (!behaviorFlags.contains(EnumerationBehavior.IGNORE_CHECK_VALVES)) {
              var oppositePistonFacing = CachedBlock.getFacing(cachedEnumeratedBlock).getOppositeFace();

              // Do not walk into the extending side of a piston - this makes it behave
              // like a check-valve, which has numerous helpful applications.
              if (oppositePistonFacing == neighborFace)
                continue;
            }

            // Pistons are treated with higher priority.
            pistonQueue.add(enumeratedBlock);
            continue;
          }

          if (!CachedBlock.isPane(cachedEnumeratedBlock)) {
            if (behaviorFlags.contains(EnumerationBehavior.DEPTH_FIRST)) {
              searchQueue.addFirst(enumeratedBlock);
              continue;
            }

            searchQueue.add(enumeratedBlock);
            continue;
          }

          var nextEnumeratedBlock = enumeratedBlock.getRelative(neighborFace);
          var cachedNextEnumeratedBlock = currentBlockCache.getCachedBlock(nextEnumeratedBlock);

          if (!CachedBlock.isValidPipeBlock(cachedNextEnumeratedBlock))
            continue;

          // Ensure that the pane is allowed to link with the block we're jumping across to
          // Same reasoning here as with [1]
          if (CachedBlock.doTubeColorsMismatch(cachedEnumeratedBlock, cachedNextEnumeratedBlock))
            continue;

          if (!visitedBlocks.add(CompactId.computeWorldlessBlockId(nextEnumeratedBlock)))
            continue;

          if (behaviorFlags.contains(EnumerationBehavior.DEPTH_FIRST)) {
            searchQueue.addFirst(nextEnumeratedBlock);
            continue;
          }

          searchQueue.add(nextEnumeratedBlock);
        }
      }

      return EnumerationResult.COMPLETED;
    } catch (LoadingChunkException e) {
      return EnumerationResult.NEEDS_CHUNK_LOADING;
    }
  }

  @Override
  public int getMaxCacheLoadCount() {
    return config.rootSection.pipes.maxCacheLoadCount;
  }

  private void startPipe(Block inputPistonBlock, @Nullable Block overrideContainerBlock, List<PipeNotification> notificationOutput) {
    this.currentBlockCache = cacheRegistry.getBlockCache(inputPistonBlock.getWorld());

    if (currentBlockCache.isBlockedDueToMinRequestTimeDelta(inputPistonBlock))
      return;

    if (currentBlockCache.isPipeOriginDisabled(inputPistonBlock))
      return;

    PipeSign sign;
    Block containerBlock;
    int cachedContainerBlock;

    try {
      int cachedInputPistonBlock = currentBlockCache.getCachedBlock(inputPistonBlock);

      if (!CachedBlock.isMaterial(cachedInputPistonBlock, Material.STICKY_PISTON))
        return;

      containerBlock = overrideContainerBlock != null ? overrideContainerBlock : inputPistonBlock.getRelative(CachedBlock.getFacing(cachedInputPistonBlock));

      if (currentBlockCache.isPipeOriginDisabled(containerBlock))
        return;

      cachedContainerBlock = currentBlockCache.getCachedBlock(containerBlock);

      sign = currentBlockCache.getSignOnPiston(inputPistonBlock, cachedInputPistonBlock, notificationOutput);
    }
    // If the very beginning of the pipe already (partially) is within an unloaded chunk,
    // there's no need to start the process at all.
    catch (LoadingChunkException ignored) {
      notificationOutput.add(new WarmupNotification(currentPistonBlockCounter, currentTubeBlockCounter));
      return;
    }

    var visitedBlocks = new LongOpenHashSet();

    visitedBlocks.add(CompactId.computeWorldlessBlockId(containerBlock));

    var otherChestBlock = CachedBlock.getOtherChestBlock(containerBlock, CachedBlock.getChestType(cachedContainerBlock), CachedBlock.getFacing(cachedContainerBlock));

    if (otherChestBlock != null) {
      if (currentBlockCache.isPipeOriginDisabled(otherChestBlock))
        return;

      visitedBlocks.add(CompactId.computeWorldlessBlockId(otherChestBlock));
    }

    var predicateEvent = new PipePredicateEvent(inputPistonBlock, sign.includeFilters, sign.excludeFilters);
    Bukkit.getPluginManager().callEvent(predicateEvent);

    // Suck items from container-block

    var pipeItems = new PipeItems();
    var suckedInventory = suckFromContainerBlockAndGetInventory(containerBlock, cachedContainerBlock, predicateEvent, pipeItems);

    if (suckedInventory == null || pipeItems.isEmptyOrNoneActive())
      return;

    // Locate exit-nodes for items

    EnumerationResult enumerationResult = null;
    var locateFlags = EnumSet.of(LocateFlag.RESET_COUNTERS);

    if (sign != PipeSign.NO_SIGN)
      locateFlags.add(LocateFlag.ENCOUNTERED_SIGN);

    var threwError = false;

    try {
      enumerationResult = locateExitNodesForItems(inputPistonBlock, visitedBlocks, locateFlags, pipeItems, notificationOutput);
    } catch (Throwable e) {
      threwError = true;
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to locate exit-nodes for an item in a pipe", e);
    }

    // Handle enumeration result

    // Do not cause leftovers to be dropped when not having encountered a sign yet during the
    // warmup process; signs are only missed if the pipe completed fully.
    var missedSign = config.rootSection.pipes.requireSign && enumerationResult == EnumerationResult.COMPLETED && !locateFlags.contains(LocateFlag.ENCOUNTERED_SIGN);

    if (missedSign)
      notificationOutput.add(new NoSignNotification());

    switch (enumerationResult) {
      case COMPLETED -> {}
      case NEEDS_CHUNK_LOADING, EXCEEDED_CACHE_LOAD_LIMIT -> notificationOutput.add(new WarmupNotification(currentPistonBlockCounter, currentTubeBlockCounter));
      case null -> {}
    }

    var shouldDropItems = (config.rootSection.pipes.dropNoSign && missedSign) || threwError;

    if (shouldDropItems) {
      pipeItems.forEachRemainingItem((_, item) -> dropItemAtBlock(item, containerBlock));
      return;
    }

    pipeItems.forEachRemainingItem((slot, item) -> {
      // This should be unreachable, seeing how nobody was able to access the
      // container while we were processing the pipe, but better safe than sorry.
      if (suckedInventory.getItem(slot) != null) {
        dropItemAtBlock(item, containerBlock);
        return;
      }

      suckedInventory.setItem(slot, item);
    });
  }

  private @Nullable Inventory suckFromContainerBlockAndGetInventory(Block containerBlock, int cachedContainerBlock, PipePredicateEvent predicateEvent, PipeItems pipeItems) {
    if (!CachedBlock.hasHandledInputInventory(cachedContainerBlock))
      return null;

    if (!(containerBlock.getState(false) instanceof InventoryHolder holder))
      return null;

    var suckedInventory = holder.getInventory();

    if (suckedInventory instanceof FurnaceInventory) {
      var result = suckedInventory.getItem(FURNACE_RESULT_INDEX);

      if (result != null && predicateEvent.testItem(result)) {
        if (pipeItems.addIfNonDuplicate(FURNACE_RESULT_INDEX, result))
          suckedInventory.setItem(FURNACE_RESULT_INDEX, null);
      }

      return suckedInventory;
    }

    if (suckedInventory instanceof BrewerInventory) {
      for (var bottleSlot = 0; bottleSlot < 3; ++bottleSlot) {
        var bottleItem = suckedInventory.getItem(bottleSlot);

        if (bottleItem != null && predicateEvent.testItem(bottleItem)) {
          if (pipeItems.addIfNonDuplicate(bottleSlot, bottleItem))
            suckedInventory.setItem(bottleSlot, null);
        }
      }

      return suckedInventory;
    }

    var inventorySize = suckedInventory.getSize();

    for (var slot = 0; slot < inventorySize; ++slot) {
      var item = suckedInventory.getItem(slot);

      if (item == null || !predicateEvent.testItem(item))
        continue;

      if (pipeItems.addIfNonDuplicate(slot, item))
        suckedInventory.setItem(slot, null);
    }

    return suckedInventory;
  }

  private void dropItemAtBlock(ItemStack itemToDrop, Block dropBlock) {
    var actualDropBlock = dropBlock;
    var nextFaceIndex = 0;

    // Try to drop the item at a block that is not going to make it shoot out due to collision
    while (nextFaceIndex < DROP_ITEM_FACES.length && !actualDropBlock.isPassable())
      actualDropBlock = dropBlock.getRelative(DROP_ITEM_FACES[nextFaceIndex++]);

    var dropLocation = actualDropBlock.getLocation().add(.5, .5, .5);
    var dropWorld = actualDropBlock.getWorld();

    dropWorld.dropItemNaturally(dropLocation, itemToDrop);
  }

  private void startPipeAndHandleNotifications(Block inputPistonBlock, @Nullable Block overrideContainerBlock) {
    var notifications = new ArrayList<PipeNotification>(1);

    pipeTimingsCommand.timeExecutionOf(inputPistonBlock, () -> startPipe(inputPistonBlock, overrideContainerBlock, notifications));

    if (notifications.isEmpty())
      return;

    var hasRegionNotifications = notifications.stream().anyMatch(notification -> notification.flags.contains(NotificationFlag.BROADCAST_TO_REGION));
    var coordinates = inputPistonBlock.getX() + " " + inputPistonBlock.getY() + " " + inputPistonBlock.getZ();

    // Avoid locating region-players if no notification targets this range, but otherwise, do use
    // this handler first, as to append region-information even to players within range.
    if (hasRegionNotifications) {
      var world = inputPistonBlock.getWorld();
      var location = inputPistonBlock.getLocation();

      WorldGuardUtil.forEachTargetedRegionPlayer(
        location,
        config.rootSection.pipes.notifications.notifyOwnersOfRegion,
        config.rootSection.pipes.notifications.notifyMembersOfRegion,
        config.rootSection.pipes.notifications.ignoredRegionsLower,
        (receiver, regionDetails) -> {
          // Do not send to players that are outside of this world - this could be rather confusing, seeing
          // how we're not printing world-names with coordinates (unnecessary clutter).
          if (!receiver.getWorld().equals(world))
            return;

          var extendedCoordinates = coordinates + " (region " + regionDetails + ")";

          for (var notification : notifications) {
            if (!notification.flags.contains(NotificationFlag.BROADCAST_TO_REGION))
              continue;

            if (!notification.addReceiver(receiver))
              continue;

            var environment = notification.buildMessageEnvironment(receiver, extendedCoordinates);
            sendNotificationIfApplicable(receiver, notification, environment, inputPistonBlock);
          }
        }
      );
    }

    var notificationRadius = config.rootSection.pipes.notifications.notificationRadius;

    if (notificationRadius > 0) {
      Location inputLocation = inputPistonBlock.getLocation();

      for (var receiver : inputPistonBlock.getWorld().getPlayers()) {
        if (receiver.getLocation().distanceSquared(inputLocation) > notificationRadius * notificationRadius)
          continue;

        for (var notification : notifications) {
          if (!notification.addReceiver(receiver))
            continue;

          var environment = notification.buildMessageEnvironment(receiver, coordinates);
          sendNotificationIfApplicable(receiver, notification, environment, inputPistonBlock);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPipeRedstone(PipeRedstoneEvent event) {
    startPipeAndHandleNotifications(event.getBlock(), null);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onHopperSearch(HopperInventorySearchEvent event) {
    // Used to let hoppers become initiators of the pipe if moveable contents
    // reside in their own inventory; handy in combination with the magnet-mechanic.

    var hopper = event.getBlock();
    var sourceOrDestination = event.getSearchBlock();

    // Destinations are always either below the hopper-block or on any other
    // direct face besides UP, whenever the output does the 90° bend. Blocks above
    // are sources, and we're not trying to suck from a pipe, merely put into it.
    if (sourceOrDestination.getY() > hopper.getY())
      return;

    startPipeAndHandleNotifications(sourceOrDestination, null);
  }

  @EventHandler
  public void onItemMove(InventoryMoveItemEvent event) {
    // Used to let hoppers become mediators for starting the pipe if moveable
    // contents reside in their connected input-container.

    var destinationLocation = event.getDestination().getLocation();

    // Move *from* a block's inventory
    if (destinationLocation == null)
      return;

    var destinationWorld = destinationLocation.getWorld();

    if (destinationWorld == null)
      return;

    var worldBlockCache = cacheRegistry.getBlockCache(destinationWorld);
    var destinationBlock = destinationLocation.getBlock();

    int cachedDestinationBlock;

    try {
      cachedDestinationBlock = worldBlockCache.getCachedBlock(destinationBlock);
    } catch (LoadingChunkException e) {
      event.setCancelled(true);
      return;
    }

    // Not moving *into* a hopper's inventory
    if (!CachedBlock.isMaterial(cachedDestinationBlock, Material.HOPPER))
      return;

    var hopperFacing = CachedBlock.getFacing(cachedDestinationBlock);

    // The block the hopper is facing into - N|E|S|W|D
    var hopperTargetBlock = destinationBlock.getRelative(hopperFacing);

    int cachedHopperTargetBlock;

    try {
      cachedHopperTargetBlock = worldBlockCache.getCachedBlock(hopperTargetBlock);
    } catch (LoadingChunkException e) {
      event.setCancelled(true);
      return;
    }

    // Not facing into a sticky-piston
    if (!CachedBlock.isMaterial(cachedHopperTargetBlock, Material.STICKY_PISTON))
      return;

    var hopperTargetFacing = CachedBlock.getFacing(cachedHopperTargetBlock);

    // The sticky-piston is not facing the output of the hopper directly
    if (hopperTargetFacing != hopperFacing.getOppositeFace())
      return;

    var sourceLocation = event.getSource().getLocation();

    if (sourceLocation == null)
      return;

    var sourceBlock = sourceLocation.getBlock();

    int cachedSourceBlock;

    try {
      cachedSourceBlock = worldBlockCache.getCachedBlock(sourceBlock);
    } catch (LoadingChunkException e) {
      event.setCancelled(true);
      return;
    }

    // Do not cancel and override behavior if we do not handle the block to be sucked from.
    // This allows, for example, the hopper to funnel bone-meal out of the composter block to then,
    // some time later, initiate a pipe-process from its very own inventory, thereby saving
    // us the hassle to directly integrate with a non-container-block directly.
    if (!CachedBlock.hasHandledInputInventory(cachedSourceBlock))
      return;

    // If all constraints of the above hold, there is no reason to move into the hopper, seeing
    // how the intention of the setup is rather unambiguous. This hopper is but a mediator.
    event.setCancelled(true);

    // #startPipe checks for target/source, so let's check for the hopper itself here as well to make it complete.
    if (worldBlockCache.isPipeOriginDisabled(destinationBlock))
      return;

    // During this event, Bukkit gets the moved item from the input-inventory and actually
    // sets its amount to 1 (or whatever's configured) by reference, so we have no way of
    // knowing how many items there actually were. Hopper-moves aren't initiated at every
    // tick (instead every 8), so we're absolutely safe to postpone this action by
    // one tick, as to get access to the unmanipulated inventory again for sucking.

    Bukkit.getScheduler().runTaskLater(plugin, () -> startPipeAndHandleNotifications(hopperTargetBlock, sourceBlock), 1);

    // Then, also call once more after 5 ticks, meaning 4 ticks later than the first attempt to suck,
    // as to make it become a steady 200ms clock if there are more items to transport, instead
    // of the 400ms as the 8 ticks would yield, which is actually noticeably slower.

    Bukkit.getScheduler().runTaskLater(plugin, () -> startPipeAndHandleNotifications(hopperTargetBlock, sourceBlock), 5);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastNotificationSendByDebounceIdByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private void sendNotificationIfApplicable(
    Player receiver,
    PipeNotification notification,
    InterpretationEnvironment environment,
    Block inputPistonBlock
  ) {
    if (isSwallowedByDebounce(receiver, notification, inputPistonBlock))
      return;

    var message = notification.getMessage(config);

    if (notification.flags.contains(NotificationFlag.SEND_IN_ACTION_BAR)) {
      message.sendActionBar(receiver, environment);
      return;
    }

    message.sendMessage(receiver, environment);
  }

  private boolean isSwallowedByDebounce(Player player, PipeNotification notification, Block inputPistonBlock) {
    var debounceId = notification.makeDebounceId(inputPistonBlock);

    if (debounceId == null)
      return false;

    var playerBucket = lastNotificationSendByDebounceIdByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> new HashMap<>());

    var lastSend = playerBucket.get(debounceId);
    var isSwallowed = lastSend != null && System.currentTimeMillis() - lastSend < config.rootSection.pipes.notifications.debounceSeconds * 1000L;

    if (isSwallowed)
      return true;

    playerBucket.put(debounceId, System.currentTimeMillis());
    return false;
  }
}
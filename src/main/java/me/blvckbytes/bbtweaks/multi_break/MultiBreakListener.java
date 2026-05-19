package me.blvckbytes.bbtweaks.multi_break;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParameters;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class MultiBreakListener implements Listener {

  private @Nullable BlockBreakEvent ignoredBreakEvent;

  private @Nullable BlockBreakEvent lastOriginBlockBreakEvent;

  private final Plugin plugin;
  private final MultiBreakParametersStore parametersStore;
  private final FurnaceLevelDisplay furnaceLevelDisplay;
  private final ConfigKeeper<MainSection> config;

  public MultiBreakListener(
    Plugin plugin,
    MultiBreakParametersStore parametersStore,
    FurnaceLevelDisplay furnaceLevelDisplay,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.parametersStore = parametersStore;
    this.furnaceLevelDisplay = furnaceLevelDisplay;
    this.config = config;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBlockDropItem(BlockDropItemEvent event) {
    if (lastOriginBlockBreakEvent == null)
      return;

    var droppingBlock = event.getBlock();
    var brokenBlock = lastOriginBlockBreakEvent.getBlock();

    if (!droppingBlock.getWorld().equals(brokenBlock.getWorld()))
      return;

    if (droppingBlock.getX() != brokenBlock.getX() || droppingBlock.getY() != brokenBlock.getY() || droppingBlock.getZ() != brokenBlock.getZ())
      return;

    lastOriginBlockBreakEvent = null;

    var pickupDelay = config.rootSection.multiBreak.customPickupDelay;

    if (pickupDelay < 0)
      return;

    for (var item : event.getItems())
      item.setPickupDelay(pickupDelay);
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (event == ignoredBreakEvent)
      return;

    var player = event.getPlayer();

    if (player.getGameMode() != GameMode.SURVIVAL)
      return;

    var parametersSlots = parametersStore.accessParametersSlots(player);

    if (parametersSlots == null)
      return;

    if (!parametersSlots.enabled)
      return;

    // In case they've been revoked permissions and are still having the extents of their prior tier set.
    if (parametersSlots.getLimits().maxDimension() == 0) {
      parametersSlots.parametersBySlotIndex.forEach(MultiBreakParameters::zeroOutAllExtents);
      parametersSlots.enabled = false;
      return;
    }

    var selectedParameters = parametersSlots.getSelectedParameters();

    if (!selectedParameters.sneakMode.doesMatch(player.isSneaking()))
      return;

    if (!config.rootSection.multiBreak.allowedWorlds.contains(player.getWorld().getName()))
      return;

    var originBlock = event.getBlock();

    if (!originBlock.isSolid())
      return;

    if (selectedParameters.filter != null && doesMaterialMismatchPredicate(selectedParameters.filter.predicate, originBlock.getType()))
      return;

    var playerInventory = player.getInventory();

    if (!DamageableHotbarItem.isRightToolForBlock(playerInventory.getItemInMainHand(), originBlock))
      return;

    lastOriginBlockBreakEvent = event;

    var directions = BlockDirections.determine(player);

    var missingToolsForBlockTypes = new HashSet<Material>();
    var excludedBlockTypes = new HashSet<Material>();

    forEachBlockWithinParameters(originBlock, directions, selectedParameters, block -> {
      if (!block.isSolid())
        return;

      var blockType = block.getType();

      if (config.rootSection.multiBreak.isBlockExcluded(blockType)) {
        excludedBlockTypes.add(blockType);
        return;
      }

      if (selectedParameters.filter != null && doesMaterialMismatchPredicate(selectedParameters.filter.predicate, blockType))
        return;

      var toolUsed = DamageableHotbarItem.determineToolFromHotbar(block, playerInventory);

      if (toolUsed == null) {
        missingToolsForBlockTypes.add(blockType);
        return;
      }

      // We're on the main thread, so there's no need for a list, seeing how events are processed strictly sequentially.
      if (ignoredBreakEvent != null)
        throw new IllegalStateException("Expected the currently ignored break-event to be null");

      var priorSlotIndex = playerInventory.getHeldItemSlot();

      // Let's also change over to the auto-selected tool, if applicable, seeing how plugins like
      // mcMMO internally often call #getItemInMainHand, which would point to the wrong item otherwise.
      if (toolUsed.slotIndex() != priorSlotIndex)
        playerInventory.setHeldItemSlot(toolUsed.slotIndex());

      var blockState = block.getState();

      //noinspection UnstableApiUsage
      var breakEvent = new BlockBreakEvent(block, player);

      var expToDrop = 0;

      if (!toolUsed.hasSilkTouch())
        expToDrop += getRandomizedExperienceForBlockType(blockType);

      if (blockState instanceof Furnace furnace)
        expToDrop += (int) furnaceLevelDisplay.calculateExperience(player, furnace.getRecipesUsed());

      breakEvent.setExpToDrop(expToDrop);

      ignoredBreakEvent = breakEvent;
      Bukkit.getPluginManager().callEvent(breakEvent);
      ignoredBreakEvent = null;

      if (breakEvent.isCancelled())
        return;

      var decreaseChance = config.rootSection.multiBreak.perAdditionalBlockDurabilityDecreaseChance;

      if (decreaseChance > 0 && Math.random() <= decreaseChance / 100.0) {
        if (!toolUsed.safelyIncrementDamageAndSet(player))
          return;
      }

      simulateBlockBreak(player, toolUsed.item(), block, blockState, breakEvent);

      if (toolUsed.slotIndex() != priorSlotIndex)
        playerInventory.setHeldItemSlot(priorSlotIndex);
    });

    if (missingToolsForBlockTypes.isEmpty() && excludedBlockTypes.isEmpty())
      return;

    var environment = new InterpretationEnvironment()
      .withVariable("missing_tools_for_blocks_type_keys", missingToolsForBlockTypes.stream().map(Material::translationKey).toList())
      .withVariable("excluded_blocks_type_keys", excludedBlockTypes.stream().map(Material::translationKey).toList());

    // Notify delayed, as to shadow jobs-messages within the action-bar.
    Bukkit.getScheduler().runTaskLater(plugin, () -> config.rootSection.multiBreak.hotbarNotification.sendActionBar(player, environment), 5);
  }

  public void simulateBlockBreak(
    Player player,
    ItemStack toolUsed,
    Block block,
    BlockState blockState,
    BlockBreakEvent breakEvent
  ) {
    var pickupDelay = config.rootSection.multiBreak.customPickupDelay;
    var world = block.getWorld();

    if (breakEvent.isDropItems()) {
      var droppedItems = block.getDrops(toolUsed, player);
      var dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

      var itemEntities = new ArrayList<Item>();

      for (ItemStack droppedItem : droppedItems) {
        var itemEntity = world.createEntity(dropLocation, Item.class);

        itemEntity.setItemStack(droppedItem);
        itemEntity.setPickupDelay(10);

        itemEntities.add(itemEntity);
      }

      //noinspection UnstableApiUsage
      var dropEvent = new BlockDropItemEvent(block, block.getState(), player, itemEntities);

      Bukkit.getPluginManager().callEvent(dropEvent);

      if (!dropEvent.isCancelled()) {

        for (Item item : dropEvent.getItems()) {
          if (pickupDelay >= 0)
            item.setPickupDelay(pickupDelay);

          if (!item.isInWorld())
            world.addEntity(item);
        }
      }
    }

    if (breakEvent.getExpToDrop() > 0) {
      var dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
      var expOrb = world.spawn(dropLocation, ExperienceOrb.class);
      expOrb.setExperience(breakEvent.getExpToDrop());
    }

    if (blockState instanceof Container container) {
      for (var containerItem : container.getInventory().getContents()) {
        if (containerItem == null || containerItem.getType().isAir())
          continue;

        var item = world.dropItem(block.getLocation(), containerItem);

        if (pickupDelay >= 0)
          item.setPickupDelay(pickupDelay);
      }
    }

    var blockData = block.getBlockData();

    player.incrementStatistic(Statistic.MINE_BLOCK, blockData.getMaterial());

    world.spawnParticle(
      Particle.BLOCK,
      block.getLocation(),
      10,
      0.33, 0.33, 0.33,
      blockData
    );

    block.setType(Material.AIR);
  }

  private void forEachBlockWithinParameters(
    Block origin,
    BlockDirections directions,
    MultiBreakParameters parameters,
    Consumer<Block> handler
  ) {
    for (var depth = 0; depth <= parameters.getExtent(BreakExtent.DEPTH); ++depth) {
      var depthOrigin = origin.getRelative(directions.forwards(), depth);

      if (depth > 0)
        handler.accept(depthOrigin);

      forEachBlockUpAndDown(depthOrigin, directions, parameters, handler);

      for (var left = 1; left <= parameters.getExtent(BreakExtent.LEFT); ++left) {
        var leftOrigin = depthOrigin.getRelative(directions.left(), left);
        handler.accept(leftOrigin);
        forEachBlockUpAndDown(leftOrigin, directions, parameters, handler);
      }

      for (var right = 1; right <= parameters.getExtent(BreakExtent.RIGHT); ++right) {
        var rightOrigin = depthOrigin.getRelative(directions.right(), right);
        handler.accept(rightOrigin);
        forEachBlockUpAndDown(rightOrigin, directions, parameters, handler);
      }
    }
  }

  private void forEachBlockUpAndDown(
    Block origin,
    BlockDirections directions,
    MultiBreakParameters parameters,
    Consumer<Block> handler
  ) {
    for (var up = 1; up <= parameters.getExtent(BreakExtent.UP); ++up)
      handler.accept(origin.getRelative(directions.up(), up));

    for (var down = 1; down <= parameters.getExtent(BreakExtent.DOWN); ++down)
      handler.accept(origin.getRelative(directions.down(), down));
  }

  private boolean doesMaterialMismatchPredicate(ItemPredicate predicate, Material material) {
    var itemType = material.asItemType();

    if (itemType == null)
      return true;

    return !predicate.test(itemType.createItemStack());
  }

  private int getRandomizedExperienceForBlockType(Material material) {
    // https://minecraft.wiki/w/Experience#Dropping_orbs
    return switch (material) {
      case NETHER_GOLD_ORE -> randomIntInRange(0, 1);
      case COAL_ORE, DEEPSLATE_COAL_ORE -> randomIntInRange(0, 2);
      case SCULK -> randomIntInRange(1, 1);
      case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> randomIntInRange(1, 5);
      case LAPIS_ORE, DEEPSLATE_LAPIS_ORE, NETHER_QUARTZ_ORE -> randomIntInRange(2, 5);
      case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> randomIntInRange(3, 7);
      case SCULK_SENSOR, SCULK_SHRIEKER, SCULK_CATALYST -> randomIntInRange(5, 5);
      case CREAKING_HEART -> randomIntInRange(20, 24);
      case SPAWNER -> randomIntInRange(15, 43);
      default -> 0;
    };
  }

  private int randomIntInRange(int minInclusive, int maxInclusive) {
    if (minInclusive == maxInclusive)
      return minInclusive;

    return minInclusive + ThreadLocalRandom.current().nextInt(maxInclusive - minInclusive + 1);
  }
}

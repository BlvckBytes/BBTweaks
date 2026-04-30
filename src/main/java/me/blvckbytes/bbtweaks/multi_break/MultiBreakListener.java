package me.blvckbytes.bbtweaks.multi_break;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParameters;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
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
import java.util.function.Consumer;

public class MultiBreakListener implements Listener {

  private @Nullable BlockBreakEvent ignoredBreakEvent;

  private @Nullable BlockBreakEvent lastOriginBlockBreakEvent;

  private final Plugin plugin;
  private final MultiBreakParametersStore parametersStore;
  private final ConfigKeeper<MainSection> config;

  public MultiBreakListener(
    Plugin plugin,
    MultiBreakParametersStore parametersStore,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.parametersStore = parametersStore;
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

    var parameters = parametersStore.accessParameters(player);

    if (parameters == null)
      return;

    if (!parameters.enabled)
      return;

    // In case they've been revoked permissions and are still having the extents of their prior tier set.
    if (parameters.getLimits().maxDimension() == 0) {
      parameters.zeroOutAllExtents();
      parameters.enabled = false;
      return;
    }

    if (!parameters.sneakMode.doesMatch(player.isSneaking()))
      return;

    if (!config.rootSection.multiBreak.allowedWorlds.contains(player.getWorld().getName()))
      return;

    var originBlock = event.getBlock();

    if (!originBlock.isSolid())
      return;

    if (parameters.filter != null && !parameters.filter.predicate.test(new ItemStack(originBlock.getType())))
      return;

    var playerInventory = player.getInventory();

    if (!DamageableHotbarItem.isRightToolForBlock(playerInventory.getItemInMainHand(), originBlock))
      return;

    lastOriginBlockBreakEvent = event;

    var directions = BlockDirections.determine(player);

    var toolsMissingForTypes = new HashSet<Material>(2);

    forEachBlockWithinParameters(originBlock, directions, parameters, block -> {
      if (!block.isSolid())
        return;

      if (parameters.filter != null && !parameters.filter.predicate.test(new ItemStack(block.getType())))
        return;

      var toolUsed = DamageableHotbarItem.determineToolFromHotbar(block, playerInventory);

      if (toolUsed == null) {
        toolsMissingForTypes.add(block.getType());
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

      //noinspection UnstableApiUsage
      var breakEvent = new BlockBreakEvent(block, player);

      // TODO: Figure out how to determine the vanilla XP-value based on the block and set it here, seeing
      //       how the default-constructor sets a value of zero, thereby not dropping anything.
      breakEvent.setExpToDrop(0);

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

      simulateBlockBreak(player, toolUsed.item(), block, breakEvent);

      if (toolUsed.slotIndex() != priorSlotIndex)
        playerInventory.setHeldItemSlot(priorSlotIndex);
    });

    if (toolsMissingForTypes.isEmpty())
      return;

    // Notify next tick, as to shadow jobs-messages within the action-bar.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var translationKeys = toolsMissingForTypes.stream().map(Material::translationKey).toList();

      config.rootSection.multiBreak.noToolsInHotbarFor.sendActionBar(
        player,
        new InterpretationEnvironment()
          .withVariable("block_type_keys", translationKeys)
      );
    }, 1);
  }

  public void simulateBlockBreak(Player player, ItemStack toolUsed, Block block, BlockBreakEvent breakEvent) {
    if (breakEvent.isDropItems()) {
      var droppedItems = block.getDrops(toolUsed, player);
      var dropLocation = block.getLocation().add(0.5, 0.5, 0.5);

      var world = block.getWorld();
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
        var pickupDelay = config.rootSection.multiBreak.customPickupDelay;

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
      var expOrb = block.getWorld().spawn(dropLocation, ExperienceOrb.class);
      expOrb.setExperience(breakEvent.getExpToDrop());
    }

    block.getWorld().spawnParticle(
      Particle.BLOCK,
      block.getLocation(),
      10,
      0.33, 0.33, 0.33,
      block.getBlockData()
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
}

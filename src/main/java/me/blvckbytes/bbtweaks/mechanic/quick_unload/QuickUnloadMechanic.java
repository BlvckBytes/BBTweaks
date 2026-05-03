package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.bbtweaks.util.MutableInt;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class QuickUnloadMechanic extends BaseMechanic<QuickUnloadInstance> implements Listener {

  public QuickUnloadMechanic(JavaPlugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, QuickUnloadInstance instance, boolean wasLeftClick) {
    var sign = instance.getSign();

    if (!canEditSign(player, sign))
      return true;

    // There's no need to print here, seeing how the instance self-destructs if that's not the case.
    if (!(instance.getMountBlock().getState() instanceof Container container))
      return true;

    if (wasLeftClick) {
      var targetInventory = container.getInventory();
      var playerInventory = player.getInventory();

      var counters = new UnloadCounters();

      if (!player.isSneaking()) {
        tryUnloadInto(playerInventory.getItemInMainHand(), targetInventory, counters);

        if (counters.encounteredContainerItems == 0) {
          config.rootSection.mechanic.quickUnload.noContainerInMainHand.sendMessage(player);
          return true;
        }

        if (counters.areTypeCountersEmpty()) {
          config.rootSection.mechanic.quickUnload.emptyContainerInMainHand.sendMessage(player);
          return true;
        }
      }

      else {
        for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
          var currentItem = playerInventory.getItem(slotIndex);

          if (currentItem == null)
            continue;

          tryUnloadInto(currentItem, targetInventory, counters);
        }

        if (counters.encounteredContainerItems == 0) {
          config.rootSection.mechanic.quickUnload.noContainerInInventory.sendMessage(player);
          return true;
        }

        if (counters.areTypeCountersEmpty()) {
          config.rootSection.mechanic.quickUnload.allContainersInInventoryAreEmpty.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("container_count", counters.encounteredContainerItems)
          );

          return true;
        }
      }

      if (counters.totalUnloadCountByType.isEmpty()) {
        config.rootSection.mechanic.quickUnload.targetInventoryIsFull.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("unfitted_items", TypeAndAmount.mapToList(counters.totalUnfittedCountByType))
            .withVariable("container_count", counters.encounteredContainerItems)
        );

        return true;
      }

      // Make sure that we also relay an update to attached comparators, hoppers and the like.
      causeBlockUpdates(instance.getMountBlock(), targetInventory);

      config.rootSection.mechanic.quickUnload.unloadProcessCompleted.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("unloaded_items", TypeAndAmount.mapToList(counters.totalUnloadCountByType))
          .withVariable("unfitted_items", TypeAndAmount.mapToList(counters.totalUnfittedCountByType))
          .withVariable("container_count", counters.encounteredContainerItems)
      );

      return true;
    }

    // Open the attached container - a simple pass-through for convenience, when misclicking.
    player.openInventory(container.getInventory());
    return true;
  }

  private void causeBlockUpdates(Block mountBlock, Inventory inventory) {
    mountBlock.getState().update(true, true);

    if (inventory instanceof DoubleChestInventory doubleChestInventory) {
      if (doubleChestInventory.getRightSide().getHolder() instanceof Container rightContainer) {
        if (!mountBlock.equals(rightContainer.getBlock())) {
          rightContainer.update(true, true);
          return;
        }
      }

      if (doubleChestInventory.getLeftSide().getHolder() instanceof Container leftContainer) {
        if (!mountBlock.equals(leftContainer.getBlock()))
          leftContainer.update(true, true);
      }
    }
  }

  private void tryUnloadInto(ItemStack item, Inventory targetInventory, UnloadCounters counters) {
    var itemMeta = item.getItemMeta();

    if (itemMeta instanceof BlockStateMeta blockStateMeta) {
      if (!(blockStateMeta.getBlockState() instanceof Container container))
        return;

      ++counters.encounteredContainerItems;

      var inventory = container.getInventory();
      var contents = inventory.getStorageContents();

      if (tryMoveItemsAndGetIfAny(contents, targetInventory, counters)) {
        inventory.setStorageContents(contents);
        blockStateMeta.setBlockState(container);
        item.setItemMeta(blockStateMeta);
      }

      return;
    }

    if (itemMeta instanceof BundleMeta bundleMeta) {
      ++counters.encounteredContainerItems;

      var contents = bundleMeta.getItems().toArray(ItemStack[]::new);

      if (tryMoveItemsAndGetIfAny(contents, targetInventory, counters)) {
        var items = new ArrayList<ItemStack>(contents.length);

        for (var content : contents) {
          if (content == null || content.getType().isAir())
            continue;

          items.add(content);
        }

        bundleMeta.setItems(items);
        item.setItemMeta(bundleMeta);
      }
    }
  }

  private boolean tryMoveItemsAndGetIfAny(ItemStack[] items, Inventory targetInventory, UnloadCounters counters) {
    var movedAnyItems = false;

    for (var slotIndex = 0; slotIndex < items.length; ++slotIndex) {
      var currentItem = items[slotIndex];

      if (currentItem == null || currentItem.getType().isAir())
        continue;

      var initialAmount = currentItem.getAmount();

      if (initialAmount <= 0)
        continue;

      var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(currentItem, targetInventory);

      var movedAmount = initialAmount - remainingAmount;

      if (movedAmount > 0) {
        counters.totalUnloadCountByType.computeIfAbsent(currentItem.getType(), k -> new MutableInt()).value += movedAmount;
        movedAnyItems = true;
      }

      if (remainingAmount > 0) {
        counters.totalUnfittedCountByType.computeIfAbsent(currentItem.getType(), k -> new MutableInt()).value += remainingAmount;
        currentItem.setAmount(remainingAmount);
        continue;
      }

      items[slotIndex] = null;
    }

    return movedAnyItems;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("QuickUnload");
  }

  @Override
  public @Nullable QuickUnloadInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.quick-unload")) {
      config.rootSection.mechanic.quickUnload.noPermission.sendMessage(creator);
      return null;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    if (!(mountBlock.getState() instanceof Container container)) {
      if (creator != null)
        config.rootSection.mechanic.quickUnload.noContainer.sendMessage(creator, environment);

      return null;
    }

    if (SignUtil.checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
      if (creator != null)
        config.rootSection.mechanic.quickUnload.existingSign.sendMessage(creator, environment);

      return null;
    }

    var instance = new QuickUnloadInstance(sign);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.quickUnload.creationSuccess.sendMessage(creator, environment);

    return instance;
  }
}

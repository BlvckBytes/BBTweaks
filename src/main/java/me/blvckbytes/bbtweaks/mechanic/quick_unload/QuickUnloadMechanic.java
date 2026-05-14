package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.TransferCounters;
import me.blvckbytes.bbtweaks.mechanic.common.TypeAndAmount;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class QuickUnloadMechanic extends PredicateMechanic<QuickUnloadInstance> {

  private static final int FLAGS_LINE = 2;

  public QuickUnloadMechanic(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    PredicateHelper predicateHelper
  ) {
    super(
      plugin, config, predicateHelper,
      new NamespacedKey(plugin, "quick-unload-filter-predicate"),
      new NamespacedKey(plugin, "quick-unload-filter-language")
    );
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

      var counters = new TransferCounters();

      if (!player.isSneaking()) {
        tryUnloadInto(playerInventory.getItemInMainHand(), targetInventory, counters, instance);

        if (counters.encounteredContainerItems == 0) {
          config.rootSection.mechanic.quickUnload.noContainerInMainHand.sendMessage(player);
          return true;
        }

        if (counters.areTypeCountersEmpty()) {
          if (counters.encounteredFilterMismatch) {
            config.rootSection.mechanic.quickUnload.mainHandContainerHasNoItemMatchingFilter.sendMessage(player);
            return true;
          }

          config.rootSection.mechanic.quickUnload.emptyContainerInMainHand.sendMessage(player);
          return true;
        }
      }

      else {
        for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
          var currentItem = playerInventory.getItem(slotIndex);

          if (currentItem == null)
            continue;

          tryUnloadInto(currentItem, targetInventory, counters, instance);
        }

        if (counters.encounteredContainerItems == 0) {
          config.rootSection.mechanic.quickUnload.noContainerInInventory.sendMessage(player);
          return true;
        }

        if (counters.areTypeCountersEmpty()) {
          if (counters.encounteredFilterMismatch) {
            config.rootSection.mechanic.quickUnload.noContainerInInventoryHasItemMatchingFilter.sendMessage(player);
            return true;
          }

          config.rootSection.mechanic.quickUnload.allContainersInInventoryAreEmpty.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("container_count", counters.encounteredContainerItems)
          );

          return true;
        }
      }

      if (!instance.flags.contains(QuickUnloadFlag.SILENT) && counters.totalTransferredCountByType.isEmpty()) {
        config.rootSection.mechanic.quickUnload.targetInventoryIsFull.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("unfitted_items", TypeAndAmount.mapToList(counters.totalExcessCountByType))
            .withVariable("container_count", counters.encounteredContainerItems)
        );

        return true;
      }

      // Make sure that we also relay an update to attached comparators, hoppers and the like.
      InventoryUtil.causeBlockUpdates(instance.getMountBlock(), targetInventory);

      if (!instance.flags.contains(QuickUnloadFlag.SILENT)) {
        config.rootSection.mechanic.quickUnload.unloadProcessCompleted.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("unloaded_items", TypeAndAmount.mapToList(counters.totalTransferredCountByType))
            .withVariable("unfitted_items", TypeAndAmount.mapToList(counters.totalExcessCountByType))
            .withVariable("container_count", counters.encounteredContainerItems)
        );
      }

      return true;
    }

    // Open the attached container - a simple pass-through for convenience, when misclicking.
    player.openInventory(container.getInventory());
    return true;
  }

  private void tryUnloadInto(ItemStack item, Inventory targetInventory, TransferCounters counters, QuickUnloadInstance instance) {
    var itemMeta = item.getItemMeta();

    if (Tag.SHULKER_BOXES.isTagged(item.getType()) && itemMeta instanceof BlockStateMeta blockStateMeta) {
      if (!(blockStateMeta.getBlockState() instanceof Container container))
        return;

      ++counters.encounteredContainerItems;

      var inventory = container.getInventory();
      var contents = inventory.getStorageContents();

      if (InventoryUtil.tryMoveItemsAndGetIfAny(contents, targetInventory, counters, null, instance.predicate)) {
        inventory.setStorageContents(contents);
        blockStateMeta.setBlockState(container);
        item.setItemMeta(blockStateMeta);
      }

      return;
    }

    if (instance.flags.contains(QuickUnloadFlag.INCLUDE_BUNDLES) && itemMeta instanceof BundleMeta bundleMeta) {
      ++counters.encounteredContainerItems;

      var contents = bundleMeta.getItems().toArray(ItemStack[]::new);

      if (InventoryUtil.tryMoveItemsAndGetIfAny(contents, targetInventory, counters, null, instance.predicate)) {
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

    var predicateAndLanguage = loadPredicateFromSign(sign);
    ItemPredicate predicate = null;

    var frontSide = sign.getSide(Side.FRONT);

    if (predicateAndLanguage != null) {
      if (!frontSide.line(0).equals(COMPONENT_PREDICATE_MODE_ON)) {
        frontSide.line(0, COMPONENT_PREDICATE_MODE_ON);
        sign.update(true, false);
      }

      predicate = predicateAndLanguage.predicate;
    }

    else {
      if (!frontSide.line(0).equals(COMPONENT_PREDICATE_MODE_OFF)) {
        frontSide.line(0, COMPONENT_PREDICATE_MODE_OFF);
        sign.update(true, false);
      }
    }

    EnumSet<QuickUnloadFlag> flags;

    try {
      flags = FlagEnum.parse(QuickUnloadFlag.class, SignUtil.getPlainTextLine(sign, FLAGS_LINE));
    } catch (UnknownFlagException exception) {
      if (creator != null)
        config.rootSection.mechanic.quickUnload.unknownFlag.sendMessage(creator, exception.makeEnvironment());

      return null;
    }

    var instance = new QuickUnloadInstance(sign, flags, predicate);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.quickUnload.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    // Let's rather not support the container too, seeing how that may be a direct pipe-input.
    return null;
  }
}

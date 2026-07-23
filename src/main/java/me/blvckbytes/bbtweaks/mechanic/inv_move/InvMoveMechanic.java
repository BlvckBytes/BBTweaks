package me.blvckbytes.bbtweaks.mechanic.inv_move;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.TransferCounters;
import me.blvckbytes.bbtweaks.mechanic.common.TypeAndAmount;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.function.IntPredicate;

public class InvMoveMechanic extends PredicateMechanic<InvMoveInstance> implements Listener {

  private static final int FLAGS_LINE = 2;

  public InvMoveMechanic(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    IPPIntegration ippIntegration
  ) {
    super(
      plugin, config, ippIntegration,
      new NamespacedKey(plugin, "inv-move-filter-predicate"),
      new NamespacedKey(plugin, "inv-move-filter-language")
    );
  }

  @Override
  public boolean onInstanceClick(Player player, InvMoveInstance instance, boolean wasLeftClick) {
    if (!canEditSign(player, instance.getSign()))
      return true;

    // There's no need to print here, seeing how the instance self-destructs if that's not the case.
    if (!(instance.getMountBlock().getState() instanceof Container container))
      return true;

    if (wasLeftClick) {
      var targetInventory = container.getInventory();
      var playerInventory = player.getInventory();

      var counters = new TransferCounters();

      var storageContents = playerInventory.getStorageContents();

      IntPredicate slotPredicate = null;

      if (instance.flags.contains(InvMoveFlag.IGNORE_HOTBAR))
        slotPredicate = slot -> slot > 8;

      if (InventoryUtil.tryMoveItemsAndGetIfAny(storageContents, targetInventory, counters, slotPredicate, instance.predicate))
        playerInventory.setStorageContents(storageContents);

      if (counters.areTypeCountersEmpty()) {
        if (counters.encounteredFilterMismatch) {
          config.rootSection.mechanic.invMove.noItemsMatchingFilterInInventory.sendMessage(player);
          return true;
        }

        config.rootSection.mechanic.invMove.noItemsInInventory.sendMessage(player);
        return true;
      }

      if (!instance.flags.contains(InvMoveFlag.SILENT) && counters.totalTransferredCountByType.isEmpty()) {
        config.rootSection.mechanic.invMove.targetInventoryIsFull.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("unfitted_items", TypeAndAmount.mapToList(counters.totalExcessCountByType))
            .withVariable("container_count", counters.encounteredContainerItems)
        );

        return true;
      }

      // Make sure that we also relay an update to attached comparators, hoppers and the like.
      InventoryUtil.causeBlockUpdates(instance.getMountBlock(), targetInventory);

      if (!instance.flags.contains(InvMoveFlag.SILENT)) {
        config.rootSection.mechanic.invMove.unloadProcessCompleted.sendMessage(
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

  @Override
  public List<String> getDiscriminators() {
    return List.of("InvMove");
  }

  @Override
  public @Nullable InvMoveInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.inv-move")) {
      config.rootSection.mechanic.invMove.noPermission.sendMessage(creator);
      return null;
    }

    var predicateAndLanguage = loadPredicateFromSign(sign);
    ItemPredicate predicate = null;

    var targetSide = sign.getSide(side);

    if (predicateAndLanguage != null) {
      if (!targetSide.line(0).equals(COMPONENT_PREDICATE_MODE_ON)) {
        targetSide.line(0, COMPONENT_PREDICATE_MODE_ON);
        sign.update(true, false);
      }

      predicate = predicateAndLanguage.predicate;
    }

    else {
      if (!targetSide.line(0).equals(COMPONENT_PREDICATE_MODE_OFF)) {
        targetSide.line(0, COMPONENT_PREDICATE_MODE_OFF);
        sign.update(true, false);
      }
    }

    EnumSet<InvMoveFlag> flags;

    try {
      flags = FlagEnum.parse(InvMoveFlag.class, SignUtil.getPlainTextLine(sign, side, FLAGS_LINE));
    } catch (UnknownFlagException exception) {
      if (creator != null)
        config.rootSection.mechanic.invMove.unknownFlag.sendMessage(creator, exception.makeEnvironment());

      return null;
    }

    var instance = new InvMoveInstance(sign, side, flags, predicate);
    var mountBlock = instance.getMountBlock();

    if (!(mountBlock.getState(false) instanceof Container container)) {
      if (creator != null)
        config.rootSection.mechanic.invMove.noContainer.sendMessage(creator, getSignEnvironment(sign));

      return null;
    }

    if (checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
      if (creator != null) {
        config.rootSection.mechanic.invMove.existingSign.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("x", mountBlock.getX())
            .withVariable("y", mountBlock.getY())
            .withVariable("z", mountBlock.getZ())
        );
      }

      return null;
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.invMove.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    // Let's rather not support the container too, seeing how that may be a direct pipe-input.
    return null;
  }
}

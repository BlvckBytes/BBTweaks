package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
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

import java.util.*;

public class AutoDisposeMechanic extends PredicateMechanic<AutoDisposeInstance> implements Listener {

  public AutoDisposeMechanic(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    IPPIntegration ippIntegration
  ) {
    super(
      plugin, config, ippIntegration,
      new NamespacedKey(plugin, "auto-dispose-filter-predicate"),
      new NamespacedKey(plugin, "auto-dispose-filter-language")
    );
  }

  @Override
  public boolean onInstanceClick(Player player, AutoDisposeInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("AutoDispose");
  }

  @Override
  public @Nullable AutoDisposeInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.auto-dispose")) {
      config.rootSection.mechanic.autoDispose.noPermission.sendMessage(creator);
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

    var instance = new AutoDisposeInstance(sign, side, predicate, config);
    var mountBlock = instance.getMountBlock();

    if (!(mountBlock.getState(false) instanceof Container container)) {
      if (creator != null)
        config.rootSection.mechanic.autoDispose.noContainer.sendMessage(creator, getSignEnvironment(sign));

      return null;
    }

    if (checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
      if (creator != null) {
        config.rootSection.mechanic.autoDispose.existingSign.sendMessage(
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
      config.rootSection.mechanic.autoDispose.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    return null;
  }
}

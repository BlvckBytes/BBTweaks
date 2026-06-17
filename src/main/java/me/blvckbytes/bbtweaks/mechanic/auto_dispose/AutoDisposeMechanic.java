package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
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
  public @Nullable AutoDisposeInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.auto-dispose")) {
      config.rootSection.mechanic.autoDispose.noPermission.sendMessage(creator);
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
        config.rootSection.mechanic.autoDispose.noContainer.sendMessage(creator, environment);

      return null;
    }

    if (SignUtil.checkIfAnyContainerSignMatches(container, this::isSignRegistered)) {
      if (creator != null)
        config.rootSection.mechanic.autoDispose.existingSign.sendMessage(creator, environment);

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

    var instance = new AutoDisposeInstance(sign, predicate, config);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.autoDispose.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    return null;
  }
}

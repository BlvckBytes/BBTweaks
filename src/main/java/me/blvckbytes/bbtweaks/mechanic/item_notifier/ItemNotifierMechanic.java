package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;
import me.blvckbytes.bbtweaks.mechanic.common.UnknownFlagException;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class ItemNotifierMechanic extends PredicateMechanic<ItemNotifierInstance> {

  private static final int NAME_LINE = 2;
  private static final int FLAGS_LINE = 3;

  public ItemNotifierMechanic(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    IPPIntegration ippIntegration
  ) {
    super(
      plugin, config, ippIntegration,
      new NamespacedKey(plugin, "item-notifier-filter-predicate"),
      new NamespacedKey(plugin, "item-notifier-filter-language")
    );
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    return null;
  }

  @Override
  public boolean onInstanceClick(Player player, ItemNotifierInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("ItemNotifier");
  }

  @Override
  public @Nullable ItemNotifierInstance onSignCreate(@Nullable Player creator, Sign sign, Side side) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.item-notifier")) {
      config.rootSection.mechanic.itemNotifier.noPermission.sendMessage(creator);
      return null;
    }

    var name = ComponentUtil.asTrimmedText(sign.getSide(side).line(NAME_LINE));

    if (name.isEmpty()) {
      if (creator != null)
        config.rootSection.mechanic.itemNotifier.missingName.sendMessage(creator, getSignEnvironment(sign));

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

    EnumSet<ItemNotifierFlag> flags;

    try {
      flags = FlagEnum.parse(ItemNotifierFlag.class, ComponentUtil.asTrimmedText(sign.getSide(side).line(FLAGS_LINE)));
    } catch (UnknownFlagException exception) {
      if (creator != null)
        config.rootSection.mechanic.itemNotifier.unknownFlag.sendMessage(creator, exception.makeEnvironment());

      return null;
    }

    var instance = new ItemNotifierInstance(sign, side, name, predicate, flags, config);

    if (!(instance.getMountBlock().getState(false) instanceof Container)) {
      if (creator != null)
        config.rootSection.mechanic.itemNotifier.noContainer.sendMessage(creator, getSignEnvironment(sign));

      return null;
    }

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.itemNotifier.creationSuccess.sendMessage(creator, getSignEnvironment(sign));

    return instance;
  }
}

package me.blvckbytes.bbtweaks.mechanic.pipe_request;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.interpreter.ValueInterpreter;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.PredicateMechanic;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PipeRequestMechanic extends PredicateMechanic<PipeRequestInstance> {

  private static final int PREDICATE_MARKER_LINE_INDEX = 0;
  private static final int MAX_STACK_COUNT_LINE_INDEX = 2;
  private static final int FLAGS_LINE_INDEX = 3;

  public static final Set<Material> supportedContainerTypes;

  static {
    supportedContainerTypes = Arrays.stream(Material.values())
      .filter(it -> Tag.SHULKER_BOXES.isTagged(it) || Tag.COPPER_CHESTS.isTagged(it) || it == Material.CHEST || it == Material.BARREL)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static final Component COMPONENT_PREDICATE_MODE_ON = Component.text("Predicate Mode").color(NamedTextColor.GREEN);
  private static final Component COMPONENT_PREDICATE_MODE_OFF = Component.empty();

  private final CacheByPosition<PipeRequestInstance> instanceByContainerPosition;

  public PipeRequestMechanic(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    PredicateHelper predicateHelper
  ) {
    super(
      plugin, config, predicateHelper,
        new NamespacedKey(plugin, "pipe-request-filter-predicate"),
        new NamespacedKey(plugin, "pipe-request-filter-language")
    );

    instanceByContainerPosition = new CacheByPosition<>();
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, PipeRequestInstance instance, boolean wasLeftClick) {
    if (wasLeftClick)
      return false;

    if (!canEditSign(player, instance.getSign()))
      return true;

    instance.request(player);

    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("PipeRequest");
  }

  @Override
  public @Nullable PipeRequestInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pipe-request")) {
      config.rootSection.mechanic.pipeRequest.noPermission.sendMessage(creator);
      return null;
    }

    var environment = getSignEnvironment(sign);

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();
    var mountBlock = signBlock.getRelative(signFacing.getOppositeFace());

    var predicateAndLanguage = loadPredicateFromSign(sign);
    ItemPredicate predicate = null;

    var frontSide = sign.getSide(Side.FRONT);

    if (predicateAndLanguage != null) {
      if (!frontSide.line(PREDICATE_MARKER_LINE_INDEX).equals(COMPONENT_PREDICATE_MODE_ON)) {
        frontSide.line(PREDICATE_MARKER_LINE_INDEX, COMPONENT_PREDICATE_MODE_ON);
        sign.update(true, false);
      }

      predicate = predicateAndLanguage.predicate;
    }

    else {
      if (!frontSide.line(PREDICATE_MARKER_LINE_INDEX).equals(COMPONENT_PREDICATE_MODE_OFF)) {
        frontSide.line(PREDICATE_MARKER_LINE_INDEX, COMPONENT_PREDICATE_MODE_OFF);
        sign.update(true, false);
      }
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, MAX_STACK_COUNT_LINE_INDEX);

    var maxStackCount = 0;

    if (!parameterLine.isBlank()) {
      var parseResult = parseExpression(parameterLine, new InterpretationEnvironment(), ValueInterpreter::asLong);

      if (parseResult.isEmpty()) {
        if (creator != null) {
          config.rootSection.mechanic.pipeRequest.maxStackCountMalformedExpression.sendMessage(
            creator,
            new InterpretationEnvironment()
              .withVariable("input", parameterLine)
          );
        }

        return null;
      }

      maxStackCount = parseResult.get().intValue();

      if (maxStackCount <= 0) {
        if (creator != null) {
          config.rootSection.mechanic.pipeRequest.maxStackCountNegativeOrZero.sendMessage(
            creator,
            new InterpretationEnvironment()
              .withVariable("input", maxStackCount)
          );
        }

        return null;
      }
    }

    EnumSet<PipeRequestFlag> flags;

    try {
      flags = PipeRequestFlag.parseFromTokens(SignUtil.getPlainTextLine(sign, FLAGS_LINE_INDEX));
    } catch (UnknownFlagException exception) {
      if (creator != null) {
        config.rootSection.mechanic.pipeRequest.unknownFlag.sendMessage(
          creator,
          environment
            .withVariable("flags", PipeRequestFlag.ALL_VALUES.stream().map(it -> it.shorthand).toList())
            .withVariable("unknown_flag", exception.unknownFlag)
        );
      }

      return null;
    }

    if (flags.contains(PipeRequestFlag.ONLY_ACKNOWLEDGE_SHULKER_IN_HAND)) {
      if (!flags.contains(PipeRequestFlag.PUT_INTO_SHULKER_BOXES)) {
        if (creator != null)
          config.rootSection.mechanic.pipeRequest.handFlagOnlyWithShulkerFlag.sendMessage(creator, environment);

        return null;
      }

      if (!(mountBlock.getState() instanceof Container)) {
        if (creator != null)
          config.rootSection.mechanic.pipeRequest.handFlagIncompatibleWithContainer.sendMessage(creator, environment);

        return null;
      }
    }

    var instance = new PipeRequestInstance(sign, predicate, maxStackCount, flags);
    var world = sign.getWorld();

    instanceBySignPosition.put(world, sign.getX(), sign.getY(), sign.getZ(), instance);
    instanceByContainerPosition.put(world, mountBlock.getX(), mountBlock.getY(), mountBlock.getZ(), instance);

    if (creator != null)
      config.rootSection.mechanic.pipeRequest.creationSuccess.sendMessage(creator, environment);

    return instance;
  }

  @Override
  public @Nullable PipeRequestInstance onSignDestroy(@Nullable Player destroyer, Sign sign) {
    var instance = super.onSignDestroy(destroyer, sign);

    if (instance != null) {
      var containerBlock = instance.getMountBlock();
      instanceByContainerPosition.invalidate(containerBlock.getWorld(), containerBlock.getX(), containerBlock.getY(), containerBlock.getZ());
    }

    return instance;
  }

  @Override
  protected @Nullable Sign tryGetSignByAuxiliaryBlock(Block block) {
    var instance = instanceByContainerPosition.get(block.getWorld(), block.getX(), block.getY(), block.getZ());

    if (instance != null)
      return instance.getSign();

    return null;
  }
}

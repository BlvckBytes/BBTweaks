package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.ast.ExpressionNode;
import at.blvckbytes.component_markup.expression.interpreter.ExpressionInterpreter;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.interpreter.ValueInterpreter;
import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.expression.tokenizer.ExpressionTokenizeException;
import at.blvckbytes.component_markup.markup.ast.tag.built_in.BuiltInTagRegistry;
import at.blvckbytes.component_markup.markup.parser.MarkupParseException;
import at.blvckbytes.component_markup.markup.parser.MarkupParser;
import at.blvckbytes.component_markup.util.InputView;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.*;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;

public abstract class BaseMechanic<InstanceType extends MechanicInstance> implements SignMechanic<InstanceType>, Listener {

  private static final BlockFace[] PRESSURE_PLATE_BELOW_FACES = {
    BlockFace.SELF, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  private static final BlockFace[] WALL_SIGN_FACES = new BlockFace[] {
    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
  };

  protected final Plugin plugin;
  protected final ConfigKeeper<MainSection> config;

  private final EnumSet<BaseMechanicFlag> flags;
  private final long interactionDebounceTicks;

  protected final CacheByPosition<InstanceType> instanceBySignPosition;

  private record LastInteraction(long time, boolean result) {}

  private final Map<UUID, LastInteraction> lastInteractionByPlayerId;

  private final Map<UUID, Long2LongMap> debounceTimeByBlockIdByPlayerId;

  private long currentTime;

  public BaseMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    BaseMechanicFlag... flags
  ) {
    this(plugin, config, 2, flags);
  }

  public BaseMechanic(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    long interactionDebounceTicks,
    BaseMechanicFlag... flags
  ) {
    if (interactionDebounceTicks < 2)
      throw new IllegalArgumentException("Interactions should be debounced at least over two ticks");

    this.plugin = plugin;
    this.config = config;
    this.interactionDebounceTicks = interactionDebounceTicks;
    this.flags = flags.length == 0 ? EnumSet.noneOf(BaseMechanicFlag.class) : EnumSet.of(flags[0], flags);

    this.instanceBySignPosition = new CacheByPosition<>();
    this.lastInteractionByPlayerId = new HashMap<>();
    this.debounceTimeByBlockIdByPlayerId = new HashMap<>();
  }

  protected boolean shouldDebounceInteraction(Player player, InstanceType instance) {
    var sign = instance.getSign();

    var bucket = debounceTimeByBlockIdByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> new Long2LongOpenHashMap());
    var blockId = CacheByPosition.computeWorldlessBlockId(sign.getX(), sign.getY(), sign.getZ());

    var previousInvocationTime = bucket.get(blockId);
    var now = getCurrentTime();

    bucket.put(blockId, now);

    return now - previousInvocationTime <= 1;
  }

  protected long getCurrentTime() {
    return currentTime;
  }

  protected InterpretationEnvironment getSignEnvironment(Sign sign) {
    return new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ());
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper == config)
      reloadAllInstances();
  }

  @Override
  public @Nullable InstanceType onSignLoad(Sign sign, Side side) {
    return onSignCreate(null, sign, side);
  }

  @Override
  public @Nullable InstanceType onSignUnload(Sign sign) {
    return onSignDestroy(null, sign);
  }

  @Override
  public @Nullable InstanceType onSignDestroy(@Nullable Player destroyer, Sign sign) {
    return instanceBySignPosition.invalidate(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
  }

  @Override
  public boolean onSignClick(Player player, Sign sign, boolean wasLeftClick) {
    var instance = instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
    return instance != null && handleInstanceClick(player, instance, wasLeftClick);
  }

  private boolean handleInstanceClick(Player player, InstanceType instance, boolean wasLeftClick) {
    var lastInteraction = lastInteractionByPlayerId.get(player.getUniqueId());

    // Debounce interaction-spam (break + interact, double-interact, etc. - no idea what's the underlying scheme with that...)
    if (lastInteraction != null && currentTime - lastInteraction.time <= interactionDebounceTicks)
      return lastInteraction.result;

    var result = onInstanceClick(player, instance, wasLeftClick);

    lastInteractionByPlayerId.put(player.getUniqueId(), new LastInteraction(currentTime, result));

    return result;
  }

  @EventHandler
  public void _onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    lastInteractionByPlayerId.remove(playerId);
    debounceTimeByBlockIdByPlayerId.remove(playerId);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void _onInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    var block = event.getClickedBlock();

    if (block == null)
      return;

    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      if (event.getHand() != EquipmentSlot.HAND)
        return;

      if (!Tag.BUTTONS.isTagged(block.getType()))
        return;

      if (!flags.contains(BaseMechanicFlag.RELAY_BUTTON))
        return;

      if (!(block.getBlockData() instanceof Directional directional))
        return;

      var signBlock = block.getRelative(directional.getFacing().getOppositeFace(), 2);

      if (signBlock.getState(false) instanceof Sign sign) {
        if (tryHandleInteraction(player, sign))
          event.setCancelled(true);

        return;
      }

      return;
    }

    if (event.getAction() == Action.PHYSICAL) {
      if (!Tag.PRESSURE_PLATES.isTagged(block.getType()))
        return;

      if (!flags.contains(BaseMechanicFlag.RELAY_PRESSURE_PLATE))
        return;

      var blockBelow = block.getRelative(0, -2, 0);

      for (var face : PRESSURE_PLATE_BELOW_FACES) {
        var signBlock = blockBelow.getRelative(face);

        if (signBlock.getState(false) instanceof Sign sign) {
          if (tryHandleInteraction(player, sign)) {
            event.setCancelled(true);
            return;
          }
        }
      }
    }
  }

  private boolean tryHandleInteraction(Player player, Sign sign) {
    var instance = instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
    return instance != null && handleInstanceClick(player, instance, false);
  }

  public boolean isSignRegistered(Sign sign) {
    return instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()) != null;
  }

  public void reloadInstanceBySign(Sign sign) {
    var instance = onSignUnload(sign);

    if (instance == null)
      return;

    if (sign.getBlock().getState() instanceof Sign newSign)
      onSignLoad(newSign, instance.getSide());
  }

  public abstract boolean onInstanceClick(Player player, InstanceType instance, boolean wasLeftClick);

  @Override
  public void disable() {
    instanceBySignPosition.clear();
  }

  @Override
  public void tick(long time) {
    currentTime = time;

    instanceBySignPosition.forEachValue(instance -> {
      var sign = instance.getSign();

      try {
        if (!instance.tick(time)) {
          sign.getBlock().breakNaturally();
          onSignDestroy(null, sign);
        }
      } catch (Throwable e) {
        var locString = sign.getX() + " " + sign.getY() + " " + sign.getZ() + " " + sign.getWorld().getName();
        plugin.getLogger().log(Level.SEVERE, "An error occurred while ticking a mechanic at " + locString, e);
      }
    });
  }

  private void reloadAllInstances() {
    var mechanics = new ArrayList<InstanceType>();

    instanceBySignPosition.forEachValue(mechanics::add);

    for (var mechanic : mechanics)
      onSignUnload(mechanic.getSign());

    for (var mechanic : mechanics) {
      var signBlock = mechanic.getSign().getBlock();

      if (!(signBlock.getState() instanceof Sign newSign))
        continue;

      if (onSignLoad(newSign, mechanic.getSide()) == null)
        signBlock.breakNaturally();
    }
  }

  protected <T> Optional<T> parseExpression(String expression, InterpretationEnvironment environment, BiFunction<ValueInterpreter, Object, T> resultMapper) {
    var inputView = InputView.of(expression);

    ExpressionNode expressionNode;

    try {
      expressionNode = ExpressionParser.parse(inputView, null);
    } catch (ExpressionTokenizeException | ExpressionParseException e) {
      return Optional.empty();
    }

    var logCount = new MutableInt();
    var value = ExpressionInterpreter.interpret(expressionNode, environment, (_, _, _, _) -> ++logCount.value);

    if (logCount.value != 0)
      return Optional.empty();

    return Optional.of(resultMapper.apply(environment.getValueInterpreter(), value));
  }

  @SuppressWarnings({"UnstableApiUsage", "BooleanMethodIsAlwaysInverted"})
  public boolean canEditSign(Player player, Sign sign) {
    var side = sign.getSide(Side.FRONT);
    var fakeEvent = new SignChangeEvent(sign.getBlock(), player, side.lines(), Side.FRONT);
    callFakeEvent(fakeEvent);
    return !fakeEvent.isCancelled();
  }

  private void callFakeEvent(Event event) {
    for(var listener : event.getHandlers().getRegisteredListeners()) {
      if (listener.getPlugin().equals(plugin))
        continue;

      try {
        listener.callEvent(event);
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + listener.getPlugin().getName(), e);
      }
    }
  }

  protected InterpretationEnvironment addErrorVariables(InterpretationEnvironment environment, MarkupParseException e) {
    return environment
      .withVariable("error_message", e.getErrorMessage())
      .withVariable("error_position", e.getErrorPosition() + 1);
  }

  protected @Nullable ComponentMarkup tryParseMarkup(String markup, Consumer<MarkupParseException> errorHandler) {
    if (markup.isBlank())
      return null;

    try {
      var ast = MarkupParser.parse(InputView.of(markup), BuiltInTagRegistry.INSTANCE);
      return new ComponentMarkup(ast, new InterpretationEnvironment(), (_, _, _, _) -> {});
    } catch (MarkupParseException e) {
      errorHandler.accept(e);
    }

    return null;
  }

  protected boolean checkIfAnyContainerSignMatches(Container container, Predicate<Sign> predicate) {
    var containerBlocks = new ArrayList<Block>(2);

    if (container.getInventory() instanceof DoubleChestInventory doubleInventory) {
      if (doubleInventory.getRightSide().getHolder(false) instanceof Container rightContainer)
        containerBlocks.add(rightContainer.getBlock());

      if (doubleInventory.getLeftSide().getHolder(false) instanceof Container leftContainer)
        containerBlocks.add(leftContainer.getBlock());
    }

    else
      containerBlocks.add(container.getBlock());

    for (var containerBlock : containerBlocks) {
      for (var signFace : WALL_SIGN_FACES) {
        var possibleSignBlock = containerBlock.getRelative(signFace);

        if (!BlockUtil.isBlockLoaded(possibleSignBlock))
          continue;

        var blockData = possibleSignBlock.getBlockData();

        if (!(Tag.WALL_SIGNS.isTagged(blockData.getMaterial())))
          continue;

        if (((Directional) blockData).getFacing() != signFace)
          continue;

        if (predicate.test((Sign) possibleSignBlock.getState(false)))
          return true;
      }

      var possibleSignBlock = containerBlock.getRelative(BlockFace.UP);

      if (!BlockUtil.isBlockLoaded(possibleSignBlock))
        continue;

      if (!Tag.STANDING_SIGNS.isTagged(possibleSignBlock.getType()))
        continue;

      if (predicate.test((Sign) possibleSignBlock.getState(false)))
        return true;
    }

    return false;
  }
}

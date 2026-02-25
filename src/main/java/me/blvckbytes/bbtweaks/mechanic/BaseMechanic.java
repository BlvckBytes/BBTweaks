package me.blvckbytes.bbtweaks.mechanic;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.ast.ExpressionNode;
import at.blvckbytes.component_markup.expression.interpreter.ExpressionInterpreter;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.interpreter.ValueInterpreter;
import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.expression.tokenizer.ExpressionTokenizeException;
import at.blvckbytes.component_markup.util.InputView;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.*;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;

public abstract class BaseMechanic<InstanceType extends MechanicInstance> implements SignMechanic<InstanceType> {

  protected final Plugin plugin;
  protected final ConfigKeeper<MainSection> config;

  protected final CacheByPosition<InstanceType> instanceBySignPosition;

  public BaseMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.plugin = plugin;
    this.config = config;

    this.instanceBySignPosition = new CacheByPosition<>();

    config.registerReloadListener(this::_onConfigReload);
  }

  protected abstract void onConfigReload();

  @Override
  public @Nullable InstanceType onSignLoad(Sign sign) {
    return onSignCreate(null, sign);
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

    if (instance != null)
      return onInstanceClick(player, instance, wasLeftClick);

    return false;
  }

  public boolean isSignRegistered(Sign sign) {
    return instanceBySignPosition.get(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ()) != null;
  }

  public void reloadInstanceBySign(Sign sign) {
    onSignUnload(sign);

    if (sign.getBlock().getState() instanceof Sign newSign)
      onSignLoad(newSign);
  }

  public abstract boolean onInstanceClick(Player player, InstanceType instance, boolean wasLeftClick);

  @Override
  public void onMechanicLoad() {}

  @Override
  public void onMechanicUnload() {
    instanceBySignPosition.clear();
  }

  @Override
  public void tick(int time) {
    instanceBySignPosition.forEachValue(instance -> {
      if (!instance.tick(time)) {
        instance.getSign().getBlock().breakNaturally();
        onSignDestroy(null, instance.getSign());
      }
    });
  }

  private void _onConfigReload() {
    var mechanicSigns = new ArrayList<Sign>();

    instanceBySignPosition.forEachValue(instance -> mechanicSigns.add(instance.getSign()));

    for (var sign : mechanicSigns)
      onSignUnload(sign);

    for (var sign : mechanicSigns) {
      var signBlock = sign.getBlock();

      if (!(signBlock.getState() instanceof Sign newSign))
        continue;

      if (onSignLoad(newSign) == null)
        signBlock.breakNaturally();
    }

    onConfigReload();
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
    var value = ExpressionInterpreter.interpret(expressionNode, environment, makeCountingLogger(logCount));

    if (logCount.value != 0)
      return Optional.empty();

    return Optional.of(resultMapper.apply(environment.getValueInterpreter(), value));
  }

  private InterpreterLogger makeCountingLogger(MutableInt output) {
    return (view, position, message, e) -> ++output.value;
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
}

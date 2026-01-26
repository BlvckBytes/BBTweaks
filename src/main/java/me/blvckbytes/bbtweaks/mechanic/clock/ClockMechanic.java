package me.blvckbytes.bbtweaks.mechanic.clock;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClockMechanic extends BaseMechanic<ClockInstance> {

  private static final int PERIOD_DURATION_LINE_INDEX = 2;

  public ClockMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, ClockInstance instance, boolean wasLeftClick) {
    var sign = instance.getSign();

    if (!canEditSign(player, sign))
      return false;

    if (!wasLeftClick || !player.isSneaking())
      return false;

    var remainingTime = instance.getRemainingTimeUntilNextToggle();

    var environment = new InterpretationEnvironment()
      .withVariable("x", sign.getX())
      .withVariable("y", sign.getY())
      .withVariable("z", sign.getZ())
      .withVariable("remaining_time", remainingTime);

    if (remainingTime < 0) {
      config.rootSection.mechanic.clock.unknownRemainingTime.sendMessage(player, environment);
      return true;
    }

    config.rootSection.mechanic.clock.remainingTimeActionBar.sendActionBar(player, environment);
    return true;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Clock");
  }

  @Override
  public @Nullable ClockInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.clock")) {
      config.rootSection.mechanic.clock.noPermission.sendMessage(creator);
      return null;
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, PERIOD_DURATION_LINE_INDEX);

    if (parameterLine.isBlank()) {
      if (creator != null)
        config.rootSection.mechanic.clock.periodDurationAbsent.sendMessage(creator);

      return null;
    }

    int periodDuration;

    try {
      periodDuration = Integer.parseInt(parameterLine);

      if (periodDuration < 0)
        throw new IllegalStateException();
    } catch (Throwable e) {
      if (creator != null) {
        config.rootSection.mechanic.clock.periodDurationNoPositiveInt.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("duration", parameterLine)
        );
      }

      return null;
    }

    if (periodDuration % 2 != 0) {
      if (creator != null) {
        config.rootSection.mechanic.clock.periodDurationUneven.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("duration", periodDuration)
        );
      }

      return null;
    }

    if (periodDuration < config.rootSection.mechanic.clock.minTickPeriod) {
      if (creator != null) {
        config.rootSection.mechanic.clock.periodDurationTooLow.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("duration", periodDuration)
            .withVariable("min_duration", config.rootSection.mechanic.clock.minTickPeriod)
        );
      }

      periodDuration = config.rootSection.mechanic.clock.minTickPeriod;
      SignUtil.setPlainTextLine(sign, PERIOD_DURATION_LINE_INDEX, String.valueOf(periodDuration), true);
    }

    var instance = new ClockInstance(periodDuration, sign);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null) {
      config.rootSection.mechanic.clock.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("duration", periodDuration)
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
      );
    }

    return instance;
  }
}

package me.blvckbytes.bbtweaks.mechanic.clock;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.BaseMechanic;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClockMechanic extends BaseMechanic<ClockInstance> {

  private static final int PERIOD_DURATION_LINE_INDEX = 2;

  public ClockMechanic(ConfigKeeper<MainSection> config) {
    super(config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public List<String> getDiscriminators() {
    return List.of("Clock");
  }

  @Override
  public boolean onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.clock")) {
      config.rootSection.mechanic.clock.noPermission.sendMessage(creator);
      return false;
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, PERIOD_DURATION_LINE_INDEX);

    if (parameterLine.isBlank()) {
      if (creator != null)
        config.rootSection.mechanic.clock.periodDurationAbsent.sendMessage(creator);

      return false;
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

      return false;
    }

    if (periodDuration % 2 != 0) {
      if (creator != null) {
        config.rootSection.mechanic.clock.periodDurationUneven.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("duration", periodDuration)
        );
      }

      return false;
    }

    if (periodDuration < config.rootSection.mechanic.clock._minTickPeriod) {
      if (creator != null) {
        config.rootSection.mechanic.clock.periodDurationTooLow.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("duration", periodDuration)
            .withVariable("min_duration", config.rootSection.mechanic.clock._minTickPeriod)
        );
      }

      periodDuration = config.rootSection.mechanic.clock._minTickPeriod;
      SignUtil.setPlainTextLine(sign, PERIOD_DURATION_LINE_INDEX, String.valueOf(periodDuration), true);
    }

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();

    instanceBySignPosition.put(
      sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(),
      new ClockInstance(periodDuration, signBlock, signFacing)
    );

    if (creator != null) {
      config.rootSection.mechanic.clock.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("duration", periodDuration)
          .withVariable("x", signBlock.getX())
          .withVariable("y", signBlock.getY())
          .withVariable("z", signBlock.getZ())
      );
    }

    return true;
  }
}

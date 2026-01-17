package me.blvckbytes.bbtweaks.mechanic.clock;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SignMechanic;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import me.blvckbytes.bbtweaks.util.IterationDecision;
import me.blvckbytes.bbtweaks.util.SignUtil;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClockMechanic implements SignMechanic {

  private static final int MIN_TICK_PERIOD = 8;

  private final ConfigKeeper<MainSection> config;
  private final CacheByPosition<ClockInstance> clockBySignPosition;

  public ClockMechanic(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.clockBySignPosition = new CacheByPosition<>();
  }

  @Override
  public void onMechanicLoad() {}

  @Override
  public void onMechanicUnload() {
    clockBySignPosition.clear();
  }

  @Override
  public void tick(int time) {
    clockBySignPosition.forEachValue(clock -> {
      clock.tick(time);
      return IterationDecision.CONTINUE;
    });
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("Clock");
  }

  @Override
  public boolean onSignLoad(Sign sign) {
    return onSignCreate(null, sign);
  }

  @Override
  public boolean onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.clock")) {
      config.rootSection.mechanic.clock.noPermission.sendMessage(creator);
      return false;
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, 2);

    if (parameterLine.isBlank()) {
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

    if (periodDuration < MIN_TICK_PERIOD) {
      config.rootSection.mechanic.clock.periodDurationTooLow.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("duration", periodDuration)
          .withVariable("min_duration", MIN_TICK_PERIOD)
      );

      return false;
    }

    if (periodDuration % 2 != 0) {
      config.rootSection.mechanic.clock.periodDurationUneven.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("duration", periodDuration)
      );

      return false;
    }

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();

    clockBySignPosition.put(
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

  @Override
  public void onSignUnload(Sign sign) {
    onSignDestroy(null, sign);
  }

  @Override
  public void onSignDestroy(@Nullable Player destroyer, Sign sign) {
    clockBySignPosition.invalidate(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ());
  }
}

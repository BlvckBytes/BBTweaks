package me.blvckbytes.bbtweaks.mechanic.pulse_extender;

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

public class PulseExtenderMechanic extends BaseMechanic<PulseExtenderInstance> {

  private static final int SIGNAL_LENGTH_LINE_INDEX = 2;

  public PulseExtenderMechanic(Plugin plugin, ConfigKeeper<MainSection> config) {
    super(plugin, config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public boolean onInstanceClick(Player player, PulseExtenderInstance instance, boolean wasLeftClick) {
    return false;
  }

  @Override
  public List<String> getDiscriminators() {
    return List.of("PulseExtender");
  }

  @Override
  public @Nullable PulseExtenderInstance onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pulse-extender")) {
      config.rootSection.mechanic.pulseExtender.noPermission.sendMessage(creator);
      return null;
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, SIGNAL_LENGTH_LINE_INDEX);

    if (parameterLine.isBlank()) {
      if (creator != null)
        config.rootSection.mechanic.pulseExtender.signalLengthAbsent.sendMessage(creator);

      return null;
    }

    int signalLength;

    try {
      signalLength = Integer.parseInt(parameterLine);

      if (signalLength < 0)
        throw new IllegalStateException();
    } catch (Throwable e) {
      if (creator != null) {
        config.rootSection.mechanic.pulseExtender.signalLengthNoPositiveInt.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("signal_length", parameterLine)
        );
      }

      return null;
    }

    if (signalLength < config.rootSection.mechanic.pulseExtender.minSignalLength) {
      if (creator != null) {
        config.rootSection.mechanic.pulseExtender.signalLengthTooLow.sendMessage(
          creator,
          new InterpretationEnvironment()
            .withVariable("signal_length", signalLength)
            .withVariable("min_signal_length", config.rootSection.mechanic.pulseExtender.minSignalLength)
        );
      }

      signalLength = config.rootSection.mechanic.pulseExtender.minSignalLength;
      SignUtil.setPlainTextLine(sign, SIGNAL_LENGTH_LINE_INDEX, String.valueOf(signalLength), true);
    }

    var instance = new PulseExtenderInstance(signalLength, sign);

    instanceBySignPosition.put(sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(), instance);

    if (creator != null) {
      config.rootSection.mechanic.pulseExtender.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("signal_length", signalLength)
          .withVariable("x", sign.getX())
          .withVariable("y", sign.getY())
          .withVariable("z", sign.getZ())
      );
    }

    return instance;
  }
}

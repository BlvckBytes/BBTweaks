package me.blvckbytes.bbtweaks.mechanic.pulse_extender;

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

public class PulseExtenderMechanic extends BaseMechanic<PulseExtenderInstance> {

  private static final int SIGNAL_LENGTH_LINE_INDEX = 2;

  public PulseExtenderMechanic(ConfigKeeper<MainSection> config) {
    super(config);
  }

  @Override
  protected void onConfigReload() {}

  @Override
  public List<String> getDiscriminators() {
    return List.of("PulseExtender");
  }

  @Override
  public boolean onSignCreate(@Nullable Player creator, Sign sign) {
    if (creator != null && !creator.hasPermission("bbtweaks.mechanic.pulse-extender")) {
      config.rootSection.mechanic.pulseExtender.noPermission.sendMessage(creator);
      return false;
    }

    var parameterLine = SignUtil.getPlainTextLine(sign, SIGNAL_LENGTH_LINE_INDEX);

    if (parameterLine.isBlank()) {
      if (creator != null)
        config.rootSection.mechanic.pulseExtender.signalLengthAbsent.sendMessage(creator);

      return false;
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

      return false;
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

    var signBlock = sign.getBlock();
    var signFacing = ((Directional) sign.getBlockData()).getFacing();

    instanceBySignPosition.put(
      sign.getWorld(), sign.getX(), sign.getY(), sign.getZ(),
      new PulseExtenderInstance(signalLength, signBlock, signFacing)
    );

    if (creator != null) {
      config.rootSection.mechanic.pulseExtender.creationSuccess.sendMessage(
        creator,
        new InterpretationEnvironment()
          .withVariable("signal_length", signalLength)
          .withVariable("x", signBlock.getX())
          .withVariable("y", signBlock.getY())
          .withVariable("z", signBlock.getZ())
      );
    }

    return true;
  }
}

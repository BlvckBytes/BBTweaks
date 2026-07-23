package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import at.blvckbytes.component_markup.util.TriState;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.Nullable;

public class TransmitterInstance extends SISOInstance {

  public final String signalName;
  public final @Nullable String namespace;
  public final String finalName;

  private TriState lastState = TriState.NULL;

  public TransmitterInstance(
    Sign sign,
    Side side,
    String signalName, @Nullable String namespace,
    String finalName
  ) {
    super(sign, side);

    this.signalName = signalName;
    this.namespace = namespace;
    this.finalName = finalName;
  }

  public TriState getLastState() {
    return lastState;
  }

  @Override
  public boolean tick(long time) {
    var inputPower = tryReadInputPower();

    if (inputPower == null) {
      lastState = TriState.NULL;
      return true;
    }

    lastState = inputPower > 0 ? TriState.TRUE : TriState.FALSE;
    return true;
  }
}

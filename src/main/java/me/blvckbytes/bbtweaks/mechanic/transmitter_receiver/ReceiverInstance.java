package me.blvckbytes.bbtweaks.mechanic.transmitter_receiver;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.Nullable;

public class ReceiverInstance extends SISOInstance {

  public final String signalName;
  public final @Nullable String namespace;
  public final String finalName;

  private boolean state;

  public ReceiverInstance(
    Sign sign,
    Side side,
    String signalName,
    @Nullable String namespace,
    String finalName
  ) {
    super(sign, side);

    this.signalName = signalName;
    this.namespace = namespace;
    this.finalName = finalName;
  }

  public void setState(boolean state) {
    this.state = state;
  }

  @Override
  public boolean tick(long time) {
    tryWriteOutputState(state);
    return true;
  }
}

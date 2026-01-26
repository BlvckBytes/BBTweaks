package me.blvckbytes.bbtweaks.mechanic.pulse_extender;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;

public class PulseExtenderInstance extends SISOInstance {

  private final int signalLength;

  private int lastSignalTime = -1;
  private int lastTickTime = -1;

  public PulseExtenderInstance(int signalLength, Sign sign) {
    super(sign);

    this.signalLength = signalLength;
  }

  @Override
  public boolean tick(int time) {
    lastTickTime = time;

    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    if (inputPower > 0)
      lastSignalTime = time;

    if (lastSignalTime < 0) {
      tryWriteOutputState(false);
      return true;
    }

    tryWriteOutputState(time - lastSignalTime <= signalLength);
    return true;
  }

  public int getRemainingHighTime() {
    if (lastTickTime < 0 || lastSignalTime < 0)
      return -1;

    var elapsedTime = lastTickTime - lastSignalTime;
    var remainingHighTime = signalLength - elapsedTime;

    if (remainingHighTime <= 0)
      return -1;

    return remainingHighTime;
  }
}

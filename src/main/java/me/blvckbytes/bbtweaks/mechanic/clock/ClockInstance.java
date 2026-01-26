package me.blvckbytes.bbtweaks.mechanic.clock;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;

public class ClockInstance extends SISOInstance {

  private final int toggleDuration;

  private int lastTickTime = -1;
  private int initTickTime = -1;

  public ClockInstance(int periodDuration, Sign sign) {
    super(sign);

    this.toggleDuration = periodDuration / 2;
  }

  @Override
  public boolean tick(int time) {
    lastTickTime = time;

    // Do not return here - begin with a high-cycle immediately, if applicable.
    if (initTickTime < 0)
      initTickTime = time;

    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    if (inputPower == 0) {
      tryWriteOutputState(false);
      initTickTime = -1;
      return true;
    }

    var elapsedTime = time - initTickTime;

    if (elapsedTime % toggleDuration != 0)
      return true;

    tryWriteOutputState(!getLastOutputState());
    return true;
  }

  public int getRemainingTimeUntilNextToggle() {
    if (lastTickTime < 0 || initTickTime < 0)
      return -1;

    var inputPower = tryReadInputPower();

    if (inputPower == null || inputPower == 0)
      return -1;

    var elapsedTime = lastTickTime - initTickTime;

    if (elapsedTime == 0)
      return -1;

    var timeRemainder = elapsedTime % toggleDuration;

    return toggleDuration - timeRemainder;
  }
}

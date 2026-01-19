package me.blvckbytes.bbtweaks.mechanic.pulse_extender;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class PulseExtenderInstance extends SISOInstance {

  private final int signalLength;

  private int lastSignalTime;

  public PulseExtenderInstance(int signalLength, Block signBlock, BlockFace signFacing) {
    super(signBlock, signFacing);

    this.signalLength = signalLength;
  }

  @Override
  public boolean tick(int time) {
    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    if (inputPower > 0)
      lastSignalTime = time;

    tryWriteOutputState(time - lastSignalTime <= signalLength);
    return true;
  }
}

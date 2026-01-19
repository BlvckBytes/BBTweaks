package me.blvckbytes.bbtweaks.mechanic.clock;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class ClockInstance extends SISOInstance {

  private final int toggleDuration;

  public ClockInstance(int periodDuration, Block signBlock, BlockFace signFacing) {
    super(signBlock, signFacing);

    this.toggleDuration = periodDuration / 2;
  }

  @Override
  public boolean tick(int time) {
    if (time % toggleDuration != 0)
      return true;

    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return true;

    if (inputPower == 0) {
      tryWriteOutputState(false);
      return true;
    }

    tryWriteOutputState(!getLastOutputState());
    return true;
  }
}

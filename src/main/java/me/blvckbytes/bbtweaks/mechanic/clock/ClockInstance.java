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
  public void tick(int time) {
    if (time % toggleDuration != 0)
      return;

    var inputPower = tryReadInputPower();

    if (inputPower == null)
      return;

    if (inputPower == 0) {
      tryWriteOutputState(false);
      return;
    }

    tryWriteOutputState(!getLastOutputState());
  }
}

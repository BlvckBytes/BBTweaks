package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;

public class AutoDisposeInstance implements MechanicInstance {

  private final Sign sign;
  private final Block mountBlock;
  private final int clearIntervalTicks;

  public AutoDisposeInstance(Sign sign, Block mountBlock, int clearIntervalTicks) {
    this.sign = sign;
    this.mountBlock = mountBlock;
    this.clearIntervalTicks = clearIntervalTicks;
  }

  @Override
  public Sign getSign() {
    return sign;
  }

  @Override
  public boolean tick(int time) {
    if (time % clearIntervalTicks != 0)
      return true;

    if (!(mountBlock.getState() instanceof Container container))
      return false;

    container.getInventory().clear();
    return true;
  }
}

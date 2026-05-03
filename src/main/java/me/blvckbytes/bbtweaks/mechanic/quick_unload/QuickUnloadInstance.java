package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;

public class QuickUnloadInstance extends SISOInstance {

  public final boolean silent;

  public QuickUnloadInstance(Sign sign, boolean silent) {
    super(sign);

    this.silent = silent;
  }

  @Override
  public boolean tick(int time) {
    if (time % 5 != 0)
      return true;

    return mountBlock.getState() instanceof Container;
  }
}

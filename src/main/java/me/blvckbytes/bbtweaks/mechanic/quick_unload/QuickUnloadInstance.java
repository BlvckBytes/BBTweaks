package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;

public class QuickUnloadInstance extends SISOInstance {

  public QuickUnloadInstance(Sign sign) {
    super(sign);
  }

  @Override
  public boolean tick(int time) {
    if (time % 5 != 0)
      return true;

    return mountBlock.getState() instanceof Container;
  }
}

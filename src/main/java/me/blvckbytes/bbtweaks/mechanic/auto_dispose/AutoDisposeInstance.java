package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;

public class AutoDisposeInstance implements MechanicInstance {

  private final Sign sign;
  private final Block mountBlock;
  private final ConfigKeeper<MainSection> config;

  public AutoDisposeInstance(Sign sign, Block mountBlock, ConfigKeeper<MainSection> config) {
    this.sign = sign;
    this.mountBlock = mountBlock;
    this.config = config;
  }

  @Override
  public Sign getSign() {
    return sign;
  }

  @Override
  public boolean tick(int time) {
    if (time % config.rootSection.mechanic.autoDispose.clearIntervalTicks != 0)
      return true;

    if (!(mountBlock.getState() instanceof Container container))
      return false;

    container.getInventory().clear();
    return true;
  }
}

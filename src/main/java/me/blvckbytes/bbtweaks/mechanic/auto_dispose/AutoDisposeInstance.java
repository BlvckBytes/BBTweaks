package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;

public class AutoDisposeInstance extends SISOInstance {

  private final ConfigKeeper<MainSection> config;

  public AutoDisposeInstance(Sign sign, ConfigKeeper<MainSection> config) {
    super(sign);
    this.config = config;
  }

  @Override
  public boolean tick(int time) {
    if (time % config.rootSection.mechanic.autoDispose.clearIntervalTicks != 0)
      return true;

    var inputPower = tryReadInputPower();

    if (inputPower == null || inputPower > 0)
      return true;

    if (!(mountBlock.getState() instanceof Container container))
      return false;

    container.getInventory().clear();
    return true;
  }
}

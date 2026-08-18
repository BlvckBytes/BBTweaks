package me.blvckbytes.bbtweaks.mechanic.wet_sponge;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class WetSpongeInstance extends SISOInstance {

  public WetSpongeInstance(
    Sign sign,
    Side side
  ) {
    super(sign, side);
  }

  @Override
  public boolean tick(long time) {
    return true;
  }
}

package me.blvckbytes.bbtweaks.mechanic.item_compress;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;

public class ItemCompressInstance extends SISOInstance {

  private static final long COMPRESS_PERIOD_T = 2;

  private final ItemCompressApi compressApi;

  public ItemCompressInstance(
    Sign sign,
    Side side,
    ItemCompressApi compressApi
  ) {
    super(sign, side);

    this.compressApi = compressApi;
  }

  @Override
  public boolean tick(long time) {
    if (time % COMPRESS_PERIOD_T != 0)
      return true;

    if (!(getMountBlock().getState(false) instanceof Container container))
      return false;

    compressApi.compressItemsInInventory(container.getInventory());
    return true;
  }
}

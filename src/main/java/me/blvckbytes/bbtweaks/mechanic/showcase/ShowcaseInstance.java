package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.mechanic.common.Offsets;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.Nullable;

public class ShowcaseInstance extends SISOInstance {

  public final @Nullable ComponentMarkup inventoryTitle;
  public final @Nullable ComponentMarkup chatMessage;

  public final Location interactionPosition;
  public final @Nullable Location containerPosition;

  public ShowcaseInstance(
    Sign sign,
    Side side,
    @Nullable ComponentMarkup inventoryTitle,
    @Nullable ComponentMarkup chatMessage,
    Offsets offsets
  ) {
    super(sign, side);

    this.inventoryTitle = inventoryTitle;
    this.chatMessage = chatMessage;

    this.interactionPosition = getMountBlock().getRelative(signFacing.getOppositeFace()).getLocation();
    this.containerPosition = offsets == Offsets.ZERO ? null : getMountBlock().getRelative(offsets.x(), offsets.y(), offsets.z()).getLocation();
  }

  @Override
  public boolean tick(long time) {
    return true;
  }
}

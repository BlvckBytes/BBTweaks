package me.blvckbytes.bbtweaks.pipes;

import me.blvckbytes.bbtweaks.mechanic.teleporter.TeleporterCoordinates;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;

import java.util.List;

public class WirelessPipeSign {

  public static final String MARKER = "[WirelessPipe]";

  private static final int MARKER_LINE_ID = 1;
  private static final int COORDINATES_LINE_ID = 2;

  public static final WirelessPipeSign NO_SIGN = new WirelessPipeSign(null, null);

  public final Block mountBlock;
  public final Block referencedBlock;

  public WirelessPipeSign(Block mountBlock, Block referencedBlock) {
    this.mountBlock = mountBlock;
    this.referencedBlock = referencedBlock;
  }

  public static WirelessPipeSign fromSign(Sign sign) {
    return fromLines(sign.getSide(Side.FRONT).lines(), sign.getBlock());
  }

  public static WirelessPipeSign fromLines(List<Component> lines, Block signBlock) {
    var markerContents = ComponentUtil.asTrimmedText(lines.get(MARKER_LINE_ID));

    if (!markerContents.equalsIgnoreCase(MARKER))
      return NO_SIGN;

    if (!(signBlock.getBlockData() instanceof Directional directional))
      return NO_SIGN;

    var coordinateContents = ComponentUtil.asTrimmedText(lines.get(COORDINATES_LINE_ID));

    // Let's just re-use the existing parser - why not.
    var coordinates = TeleporterCoordinates.tryParse(coordinateContents);

    if (coordinates == null)
      return NO_SIGN;

    var referencedBlock = signBlock.getWorld().getBlockAt((int) coordinates.x(), (int) coordinates.y(), (int) coordinates.z());
    var mountBlock = signBlock.getRelative(directional.getFacing().getOppositeFace());

    return new WirelessPipeSign(mountBlock, referencedBlock);
  }
}

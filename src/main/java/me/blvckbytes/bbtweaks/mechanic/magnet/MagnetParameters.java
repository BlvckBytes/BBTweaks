package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.util.SignUtil;
import me.blvckbytes.bbtweaks.util.StringUtil;
import org.bukkit.block.Sign;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public class MagnetParameters {

  private static final int EXTENTS_LINE_INDEX = 2;
  private static final int OFFSETS_LINE_INDEX = 3;

  public final Sign sign;

  private final MagnetParameter[] parameters;

  public final MagnetParameter extentX;
  public final MagnetParameter extentY;
  public final MagnetParameter extentZ;

  public final MagnetParameter offsetX;
  public final MagnetParameter offsetY;
  public final MagnetParameter offsetZ;

  public MagnetParameters(Sign sign, ConfigKeeper<MainSection> config) {
    this.sign = sign;

    this.extentX = new MagnetParameter("EXTENT_X", config.rootSection.mechanic.magnet.defaultWidthAndDepth, clampMinOneMaxValue(config.rootSection.mechanic.magnet.maxWidthOrDepth));
    this.extentY = new MagnetParameter("EXTENT_Y", config.rootSection.mechanic.magnet.defaultHeight, clampMinOneMaxValue(config.rootSection.mechanic.magnet.maxHeight));
    this.extentZ = new MagnetParameter("EXTENT_Z", config.rootSection.mechanic.magnet.defaultWidthAndDepth, clampMinOneMaxValue(config.rootSection.mechanic.magnet.maxWidthOrDepth));

    this.offsetX = new MagnetParameter("OFFSET_X", config.rootSection.mechanic.magnet.defaultOffsetX, clampMinNegativeSupplierExclusiveMaxZero(extentX::getValue));
    this.offsetY = new MagnetParameter("OFFSET_Y", config.rootSection.mechanic.magnet.defaultOffsetY, clampMinNegativeSupplierExclusiveMaxZero(extentY::getValue));
    this.offsetZ = new MagnetParameter("OFFSET_Z", config.rootSection.mechanic.magnet.defaultOffsetZ, clampMinNegativeSupplierExclusiveMaxZero(extentZ::getValue));

    this.parameters = new MagnetParameter[] {
      extentX, extentY, extentZ,
      offsetX, offsetY, offsetZ,
    };
  }

  public void forEach(Consumer<MagnetParameter> handler) {
    for (var parameter : parameters)
      handler.accept(parameter);
  }

  public void updateAll() {
    for (var parameter : parameters)
      parameter.setValueAndGetIfValid(parameter.getValue());
  }

  public Cuboid makeCuboid() {
    var minBlock = sign.getBlock().getRelative(offsetX.getValue(), offsetY.getValue(), offsetZ.getValue());
    var maxBlock = minBlock.getRelative(extentX.getValue(), extentY.getValue(), extentZ.getValue());
    return new Cuboid(minBlock, maxBlock);
  }

  private String getOrEmpty(List<String> values, int index) {
    if (index < 0 || index >= values.size())
      return "";

    return values.get(index);
  }

  public void read() {
    var extentsTokens = StringUtil.getTokens(SignUtil.getPlainTextLine(sign, EXTENTS_LINE_INDEX));

    extentX.readFromToken(getOrEmpty(extentsTokens, 0));
    extentY.readFromToken(getOrEmpty(extentsTokens, 1));
    extentZ.readFromToken(getOrEmpty(extentsTokens, 2));

    var offsetsTokens = StringUtil.getTokens(SignUtil.getPlainTextLine(sign, OFFSETS_LINE_INDEX));

    offsetX.readFromToken(getOrEmpty(offsetsTokens, 0));
    offsetY.readFromToken(getOrEmpty(offsetsTokens, 1));
    offsetZ.readFromToken(getOrEmpty(offsetsTokens, 2));
  }

  public boolean writeIfDirty(boolean updateIfDirty) {
    var isDirty = false;

    if (extentX.isDirtySinceLastRead() || extentY.isDirtySinceLastRead() || extentZ.isDirtySinceLastRead()) {
      SignUtil.setPlainTextLine(sign, EXTENTS_LINE_INDEX, extentX.getValue() + " " + extentY.getValue() + " " + extentZ.getValue(), false);
      isDirty = true;
    }

    if (offsetX.isDirtySinceLastRead() || offsetY.isDirtySinceLastRead() || offsetZ.isDirtySinceLastRead()) {
      SignUtil.setPlainTextLine(sign, OFFSETS_LINE_INDEX, offsetX.getValue() + " " + offsetY.getValue() + " " + offsetZ.getValue(), false);
      isDirty = true;
    }

    if (updateIfDirty && isDirty)
      sign.update(true, false);

    return isDirty;
  }

  public MagnetParameter getFirst() {
    return parameters[0];
  }

  private ParameterClamp clampMinOneMaxValue(int max) {
    return value -> Math.max(1, Math.min(value, max));
  }

  private ParameterClamp clampMinNegativeSupplierExclusiveMaxZero(IntSupplier min) {
    return value -> Math.max(-(min.getAsInt() - 1), Math.min(value, 0));
  }
}

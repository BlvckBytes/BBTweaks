package me.blvckbytes.bbtweaks.inv_magnet.parameters;

public record InvMagnetLimits(
  int maxRadius,
  String tierName
) {

  public static final InvMagnetLimits ZERO = new InvMagnetLimits(0, null);

}

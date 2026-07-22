package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import net.kyori.adventure.text.Component;

public record InvMagnetLimits(
  int maxRadius,
  String tierName,
  String worldGroupIdentifyingName,
  Component worldGroupDisplayName
) {

  public static final InvMagnetLimits ZERO = new InvMagnetLimits(0, null, null, null);

  public boolean isZero() {
    return this == ZERO || maxRadius == 0;
  }
}

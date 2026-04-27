package me.blvckbytes.bbtweaks.multi_break.config;

public record MultiBreakLimits(
  int maxVolume,
  int maxExtent,
  String tierName
) {

  public static final MultiBreakLimits ZERO = new MultiBreakLimits(0, 0, null);

}

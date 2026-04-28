package me.blvckbytes.bbtweaks.multi_break.config;

public record MultiBreakLimits(
  int maxDimension,
  String tierName
) {

  public static final MultiBreakLimits ZERO = new MultiBreakLimits(0, null);

}

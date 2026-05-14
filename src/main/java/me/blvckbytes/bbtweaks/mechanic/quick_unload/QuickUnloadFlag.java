package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum QuickUnloadFlag implements FlagEnum {
  SILENT("silent"),
  ;

  private final String shorthand;

  QuickUnloadFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

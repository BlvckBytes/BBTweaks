package me.blvckbytes.bbtweaks.teleporter_sign;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum TeleporterSignFlag implements FlagEnum {
  NO_BACK("no-back"),
  SILENT("silent"),
  NORTH("north"),
  EAST("east"),
  SOUTH("south"),
  WEST("west"),
  ;

  private final String shorthand;

  TeleporterSignFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

package me.blvckbytes.bbtweaks.mechanic.teleporter;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum TeleporterFlag implements FlagEnum {
  NO_BACK("no-back"),
  SILENT("silent"),
  NORTH("north"),
  EAST("east"),
  SOUTH("south"),
  WEST("west"),
  NORTH_EAST("north-east"),
  NORTH_WEST("north-west"),
  SOUTH_EAST("south-east"),
  SOUTH_WEST("south-west")
  ;

  private final String shorthand;

  TeleporterFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

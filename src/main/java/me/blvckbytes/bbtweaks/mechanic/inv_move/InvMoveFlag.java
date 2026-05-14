package me.blvckbytes.bbtweaks.mechanic.inv_move;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum InvMoveFlag implements FlagEnum {
  SILENT("silent"),
  IGNORE_HOTBAR("-hotbar"),
  ;

  private final String shorthand;

  InvMoveFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

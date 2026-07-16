package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum AutoCrafterFlag implements FlagEnum {
  USE_SLOT_STATE_AS_PATTERN("slot-pattern"),
  ENABLE_META_RECIPE("meta"),
  ;

  private final String shorthand;

  AutoCrafterFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

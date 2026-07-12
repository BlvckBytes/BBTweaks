package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import me.blvckbytes.bbtweaks.mechanic.common.FlagEnum;

public enum ItemNotifierFlag implements FlagEnum {
  ADD("add"),
  REMOVE("remove"),
  FULL_SLOTS("full-slots"),
  FULL_STACKS("full-stacks"),
  EMPTY("empty"),
  OWNER("owner"),
  MEMBER("member"),
  ;

  private final String shorthand;

  ItemNotifierFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  @Override
  public String getShorthand() {
    return shorthand;
  }
}

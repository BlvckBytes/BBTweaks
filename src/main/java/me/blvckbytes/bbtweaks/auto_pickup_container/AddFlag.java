package me.blvckbytes.bbtweaks.auto_pickup_container;

import java.util.EnumSet;

public enum AddFlag {
  ALLOW_UNMARKED,
  ;

  public static EnumSet<AddFlag> makeSet(AddFlag... flags) {
    if (flags.length == 0)
      return EnumSet.noneOf(AddFlag.class);

    return EnumSet.of(flags[0], flags);
  }
}

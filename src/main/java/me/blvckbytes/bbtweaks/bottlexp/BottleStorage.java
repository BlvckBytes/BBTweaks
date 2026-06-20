package me.blvckbytes.bbtweaks.bottlexp;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum BottleStorage implements MatchableEnum {
  INV(true, false),
  SHULKER(false, true),
  INV_SHULKER(true, true),
  ;

  public static final BottleStorage DEFAULT_VALUE = INV;

  public static final EnumMatcher<BottleStorage> matcher = new EnumMatcher<>(values());

  public final boolean intoInventory;
  public final boolean intoShulkers;

  BottleStorage(boolean intoInventory, boolean intoShulkers) {
    this.intoInventory = intoInventory;
    this.intoShulkers = intoShulkers;
  }
}

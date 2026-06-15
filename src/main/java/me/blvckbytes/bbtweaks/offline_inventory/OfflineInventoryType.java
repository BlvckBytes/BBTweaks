package me.blvckbytes.bbtweaks.offline_inventory;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum OfflineInventoryType implements MatchableEnum {
  PLAYER_INVENTORY,
  ENDER_CHEST,
  ;

  public static final EnumMatcher<OfflineInventoryType> matcher = new EnumMatcher<>(values());
}

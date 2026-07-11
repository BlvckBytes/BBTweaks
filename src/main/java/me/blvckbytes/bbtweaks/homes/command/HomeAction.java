package me.blvckbytes.bbtweaks.homes.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum HomeAction implements MatchableEnum {
  MOVE_HERE,
  RENAME,
  SET_ICON,
  REMOVE_ICON,
  MARK_FAVORITE,
  REMOVE_FAVORITE,
  ;

  public static final EnumMatcher<HomeAction> matcher = new EnumMatcher<>(values());
}

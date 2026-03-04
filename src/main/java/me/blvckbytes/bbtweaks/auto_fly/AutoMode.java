package me.blvckbytes.bbtweaks.auto_fly;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum AutoMode implements MatchableEnum {
  ENABLED,
  ENABLED_SET_FLYING,
  OFF,
  ;

  public static final EnumMatcher<AutoMode> matcher = new EnumMatcher<>(values());
}

package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum PredicateMode implements MatchableEnum {
  ALLOW,
  DENY,
  OFF,
  ;

  public static final EnumMatcher<PredicateMode> matcher = new EnumMatcher<>(values());

}

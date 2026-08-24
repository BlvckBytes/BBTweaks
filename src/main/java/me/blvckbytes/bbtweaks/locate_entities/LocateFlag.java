package me.blvckbytes.bbtweaks.locate_entities;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum LocateFlag implements MatchableEnum {
  IGNORE_LOBOTOMIZED,
  ;

  public static final EnumMatcher<LocateFlag> matcher = new EnumMatcher<>(values());

}

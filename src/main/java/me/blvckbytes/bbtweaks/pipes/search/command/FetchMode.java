package me.blvckbytes.bbtweaks.pipes.search.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum FetchMode implements MatchableEnum {
  FIRST,
  EVERY,
  ;

  public static final EnumMatcher<FetchMode> matcher = new EnumMatcher<>(values());

}

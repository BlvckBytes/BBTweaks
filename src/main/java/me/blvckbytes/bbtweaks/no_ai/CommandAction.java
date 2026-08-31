package me.blvckbytes.bbtweaks.no_ai;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  STATUS,
  MAKE_LOOK,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

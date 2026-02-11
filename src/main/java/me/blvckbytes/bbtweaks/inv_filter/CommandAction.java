package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SET_FILTER,
  SET_FILTER_WITH_LANGUAGE,
  ENABLE,
  DISABLE,
  ;

  static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

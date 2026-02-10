package me.blvckbytes.bbtweaks.inv_filter;

import me.blvckbytes.item_predicate_parser.syllables_matcher.EnumMatcher;
import me.blvckbytes.item_predicate_parser.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SET,
  SET_LANGUAGE,
  MODE,
  ;

  static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

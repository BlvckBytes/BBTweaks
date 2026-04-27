package me.blvckbytes.bbtweaks.multi_break.command;

import me.blvckbytes.item_predicate_parser.syllables_matcher.EnumMatcher;
import me.blvckbytes.item_predicate_parser.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  ON,
  OFF,
  SIZE,
  SET_FILTER,
  SET_FILTER_WITH_LANGUAGE,
  GET_FILTER,
  REMOVE_FILTER,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

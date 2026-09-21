package me.blvckbytes.bbtweaks.multi_break.command;

import me.blvckbytes.item_predicate_parser.syllables_matcher.EnumMatcher;
import me.blvckbytes.item_predicate_parser.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  ON,
  OFF,
  TOGGLE,
  SIZE,
  SET_FILTER,
  SET_FILTER_WITH_LANGUAGE,
  GET_FILTER,
  REMOVE_FILTER,
  ENABLE_FILTER,
  DISABLE_FILTER,
  TOGGLE_FILTER,
  SELECT_SLOT,
  SET_MIN_Y,
  REMOVE_MIN_Y,
  SET_MAX_Y,
  REMOVE_MAX_Y,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

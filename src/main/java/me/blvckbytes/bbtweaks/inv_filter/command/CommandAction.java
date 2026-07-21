package me.blvckbytes.bbtweaks.inv_filter.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  GET_FILTER,
  SET_FILTER,
  SET_FILTER_WITH_LANGUAGE,
  REMOVE_FILTER,
  ON,
  OFF,
  TOGGLE,
  SELECT_SLOT,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

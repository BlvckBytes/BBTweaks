package me.blvckbytes.bbtweaks.pipes.predicates.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SEARCH,
  GET,
  SET,
  SET_LANGUAGE,
  REMOVE,
  GENERATE,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

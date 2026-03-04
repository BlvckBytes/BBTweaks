package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SET_PASSWORD,
  GET_PASSWORD,
  REMOVE_PASSWORD,
  ALLOW_KEY_OR_PASSWORD,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

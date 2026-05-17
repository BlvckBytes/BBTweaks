package me.blvckbytes.bbtweaks.durability_warnings.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  TOGGLE_SOUND,
  TOGGLE_ENABLED,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

package me.blvckbytes.bbtweaks.sidebar.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SETTINGS,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

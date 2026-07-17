package me.blvckbytes.bbtweaks.block_facing.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  ON,
  OFF,
  TOGGLE,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

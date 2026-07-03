package me.blvckbytes.bbtweaks.block_facing.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  PLACING_ON,
  PLACING_OFF,
  PLACING_TOGGLE,
  EXISTING_ON,
  EXISTING_OFF,
  EXISTING_TOGGLE,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

package me.blvckbytes.bbtweaks.inv_magnet.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  ON,
  OFF,
  TOGGLE,
  RADIUS,
  STATUS,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

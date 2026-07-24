package me.blvckbytes.bbtweaks.auto_pickup_container.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  ON,
  OFF,
  TOGGLE,
  OVERVIEW,
  CAPACITY_WARNING,
  ;

  public static EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

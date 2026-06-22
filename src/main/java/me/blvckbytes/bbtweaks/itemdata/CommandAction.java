package me.blvckbytes.bbtweaks.itemdata;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  INV,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

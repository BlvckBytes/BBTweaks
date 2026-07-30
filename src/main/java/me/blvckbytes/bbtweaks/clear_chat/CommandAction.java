package me.blvckbytes.bbtweaks.clear_chat;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  SELF,
  OTHER,
  GLOBAL,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());

}

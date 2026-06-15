package me.blvckbytes.bbtweaks.sign_copier;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  EDIT,
  EDIT_PLAIN,
  PREVIEW,
  CLEAR,
  SETTINGS,
  COPY,
  PASTE,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}

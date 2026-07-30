package me.blvckbytes.bbtweaks.clear_chat;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

import java.util.EnumSet;
import java.util.List;

public enum CommandFlag implements MatchableEnum {
  SILENT
  ;

  public static final EnumMatcher<CommandFlag> matcher = new EnumMatcher<>(values());

  public static List<String> createCompletions(String[] args, EnumSet<CommandFlag> flags) {
    return CommandFlag.matcher.createCompletions(args[args.length - 1], flag -> !flags.contains(flag.constant));
  }
}

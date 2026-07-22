package me.blvckbytes.bbtweaks.pipes.search.command;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

import java.util.EnumSet;
import java.util.List;

public enum CommandFlag implements MatchableEnum {
  HONOR_CHECK_VALVES,
  IGNORE_NON_STORAGE,
  ;

  public static final EnumMatcher<CommandFlag> matcher = new EnumMatcher<>(values());

  public static EnumSet<CommandFlag> consumeLeadingFlags(List<String> args) throws UnknownCommandFlagException {
    var result = EnumSet.noneOf(CommandFlag.class);

    while (!args.isEmpty()) {
      var arg = args.getFirst();

      if (!arg.startsWith("-") || arg.length() == 1)
        break;

      var flag = matcher.matchFirst(arg.substring(1));

      if (flag == null)
        throw new UnknownCommandFlagException(arg);

      args.removeFirst();

      result.add(flag.constant);
    }

    return result;
  }
}

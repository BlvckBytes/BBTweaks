package me.blvckbytes.bbtweaks.un_craft;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public enum CommandFlag {
  ALL_MODE('a'),
  // TODO: Implement me next, :)
//  ACCEPT_REDUCED('r'),
  ;

  public record ArgsAndFlags(List<String> args, EnumSet<CommandFlag> flags) {}

  private static final Map<Character, CommandFlag> flagByCharLower;

  static {
    flagByCharLower = new HashMap<>();

    for (var value : values())
      flagByCharLower.put(Character.toLowerCase(value.character), value);
  }

  public final char character;
  public final String representation;

  CommandFlag(char c) {
    this.character = c;
    this.representation = "-" + c;
  }

  public static @Nullable CommandFlag getFlagByChar(char c) {
    return flagByCharLower.get(Character.toLowerCase(c));
  }

  public static ArgsAndFlags parseFlags(String[] args) {
    var remainingArgs = new ArrayList<String>();
    var flags = EnumSet.noneOf(CommandFlag.class);

    for (var arg : args) {
      if (arg.startsWith("-")) {
        for (var charIndex = 1; charIndex < arg.length(); ++charIndex) {
          var flag = getFlagByChar(arg.charAt(charIndex));

          if (flag != null)
            flags.add(flag);
        }
        continue;
      }

      remainingArgs.add(arg);
    }

    return new ArgsAndFlags(remainingArgs, flags);
  }
}

package me.blvckbytes.bbtweaks.mechanic.pipe_request;

import java.util.EnumSet;
import java.util.List;

public enum PipeRequestFlag {
  PUT_INTO_SHULKER_BOXES("shulker"),
  ONLY_ACKNOWLEDGE_SHULKER_IN_HAND("hand"),
  ;

  public static final List<PipeRequestFlag> ALL_VALUES = List.of(values());

  public final String shorthand;

  PipeRequestFlag(String shorthand) {
    this.shorthand = shorthand;
  }

  public static EnumSet<PipeRequestFlag> parseFromTokens(String line) throws UnknownFlagException {
    var result = EnumSet.noneOf(PipeRequestFlag.class);

    tokenLoop:
    for (var token : line.split("\\s+")) {
      if (token.isBlank())
        continue;

      for (var flag : ALL_VALUES) {
        if (flag.shorthand.equalsIgnoreCase(token)) {
          result.add(flag);
          continue tokenLoop;
        }
      }

      throw new UnknownFlagException(token);
    }

    return result;
  }
}

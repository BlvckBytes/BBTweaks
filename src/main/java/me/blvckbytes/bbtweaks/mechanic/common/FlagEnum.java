package me.blvckbytes.bbtweaks.mechanic.common;

import java.util.EnumSet;

public interface FlagEnum {

  String getShorthand();

  static <T extends Enum<T> & FlagEnum> EnumSet<T> parse(Class<T> enumClass, String... tokenLines) throws UnknownFlagException {
    var result = EnumSet.noneOf(enumClass);

    for (var tokenLine : tokenLines) {
      tokenLoop:
      for (var token : tokenLine.split(" ")) {
        if (token.isBlank())
          continue;

        for (var enumConstant : enumClass.getEnumConstants()) {
          if (token.equalsIgnoreCase(enumConstant.getShorthand())) {
            result.add(enumConstant);
            continue tokenLoop;
          }
        }

        throw UnknownFlagException.make(token, enumClass);
      }
    }

    return result;
  }
}

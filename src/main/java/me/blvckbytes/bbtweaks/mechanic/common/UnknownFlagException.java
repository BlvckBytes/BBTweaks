package me.blvckbytes.bbtweaks.mechanic.common;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;

import java.util.Arrays;
import java.util.List;

public class UnknownFlagException extends Exception {

  public final String unknownFlag;
  public final List<String> knownFlags;

  private UnknownFlagException(String unknownFlag, List<String> knownFlags) {
    this.unknownFlag = unknownFlag;
    this.knownFlags = knownFlags;
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("unknown_flag", unknownFlag)
      .withVariable("known_flags", knownFlags);
  }

  public static <T extends Enum<T> & FlagEnum> UnknownFlagException make(String unknownFlag, Class<T> enumClass) {
    return new UnknownFlagException(
      unknownFlag,
      Arrays.stream(enumClass.getEnumConstants()).map(FlagEnum::getShorthand).toList()
    );
  }
}

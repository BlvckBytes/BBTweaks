package me.blvckbytes.bbtweaks.multi_break.parameters;

import java.util.Arrays;
import java.util.List;

public enum SneakMode {
  NONE,
  ENABLE_WHILE_SNEAKING,
  DISABLE_WHILE_SNEAKING,
  ;

  private static final List<SneakMode> values = Arrays.asList(values());

  public SneakMode next() {
    return values.get((ordinal() + 1) % values.size());
  }

  public boolean doesMatch(boolean isSneaking) {
    return switch (this) {
      case NONE -> true;
      case ENABLE_WHILE_SNEAKING -> isSneaking;
      case DISABLE_WHILE_SNEAKING -> !isSneaking;
    };
  }

  public static SneakMode byOrdinalOrFirst(int ordinal) {
    if (ordinal <= 0 || ordinal >= values.size())
      return values.getFirst();

    return values.get(ordinal);
  }
}

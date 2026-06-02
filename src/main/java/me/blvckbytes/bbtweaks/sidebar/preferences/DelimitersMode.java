package me.blvckbytes.bbtweaks.sidebar.preferences;

import java.util.List;

public enum DelimitersMode {
  // NOTE: The ordinal of this enum is used as the main identifier!
  NONE,
  TOP_AND_BOTTOM,
  TOP_ONLY,
  ;

  public static final List<DelimitersMode> ALL_VALUES = List.of(values());
  public static final DelimitersMode DEFAULT_VALUE = TOP_AND_BOTTOM;

  public DelimitersMode next() {
    return ALL_VALUES.get((ordinal() + 1) % ALL_VALUES.size());
  }

  public static DelimitersMode byOrdinalOrDefault(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return DEFAULT_VALUE;

    return ALL_VALUES.get(ordinal);
  }
}

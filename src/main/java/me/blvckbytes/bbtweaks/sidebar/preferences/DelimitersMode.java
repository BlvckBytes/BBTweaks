package me.blvckbytes.bbtweaks.sidebar.preferences;

import java.util.List;

public enum DelimitersMode {
  // NOTE: The ordinal of this enum is used as the main identifier!
  NONE,
  TOP_AND_BOTTOM,
  TOP_ONLY,
  ;

  public static final List<DelimitersMode> ALL_VALUES = List.of(values());

  public DelimitersMode next() {
    return ALL_VALUES.get((ordinal() + 1) % ALL_VALUES.size());
  }

  public static DelimitersMode byOrdinalOrDefault(int ordinal, SidebarDefaultsSection defaults) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return defaults.delimitersMode;

    return ALL_VALUES.get(ordinal);
  }
}

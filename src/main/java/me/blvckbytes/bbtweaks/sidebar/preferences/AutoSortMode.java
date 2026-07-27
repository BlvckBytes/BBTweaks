package me.blvckbytes.bbtweaks.sidebar.preferences;

import java.util.List;

public enum AutoSortMode {
  // NOTE: The ordinal of this enum is used as the main identifier!
  OFF,
  ASCENDING,
  DESCENDING,
  ;

  public static final List<AutoSortMode> ALL_VALUES = List.of(values());

  public AutoSortMode next() {
    return ALL_VALUES.get((ordinal() + 1) % ALL_VALUES.size());
  }

  public static AutoSortMode byOrdinalOrDefault(int ordinal, SidebarDefaultsSection defaults) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return defaults.autoSortMode;

    return ALL_VALUES.get(ordinal);
  }
}

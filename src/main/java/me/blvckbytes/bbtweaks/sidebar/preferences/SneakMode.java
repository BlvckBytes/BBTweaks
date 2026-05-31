package me.blvckbytes.bbtweaks.sidebar.preferences;

import java.util.List;

public enum SneakMode {
  // NOTE: The ordinal of this enum is used as the main identifier!
  NONE,
  DOUBLE_SNEAK_TOGGLES,
  ENABLE_DURING_SNEAK,
  DISABLE_DURING_SNEAK,
  ;

  public static final List<SneakMode> ALL_VALUES = List.of(values());

  public SneakMode next() {
    return ALL_VALUES.get((ordinal() + 1) % ALL_VALUES.size());
  }

  public static SneakMode byOrdinalOrDefault(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return NONE;

    return ALL_VALUES.get(ordinal);
  }
}

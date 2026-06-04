package me.blvckbytes.bbtweaks.sidebar.preferences;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum StatisticEnableMode {
  ON(true, true),
  VALUE_ONLY(true, false),
  OFF(false, false),
  ;

  public final boolean enabled;
  public final boolean showLabel;

  public static final List<StatisticEnableMode> ALL_VALUES = List.of(values());

  StatisticEnableMode(boolean enabled, boolean showLabel) {
    this.enabled = enabled;
    this.showLabel = showLabel;
  }

  public StatisticEnableMode next() {
    return ALL_VALUES.get((ordinal() + 1) % ALL_VALUES.size());
  }

  public static @Nullable StatisticEnableMode byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }

  public static StatisticEnableMode fromBoolean(boolean value) {
    return value ? ON : OFF;
  }
}

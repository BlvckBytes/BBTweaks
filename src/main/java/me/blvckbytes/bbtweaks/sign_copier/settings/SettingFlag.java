package me.blvckbytes.bbtweaks.sign_copier.settings;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum SettingFlag {
  // NOTE: The ordinal of this enum is used as the main identifier!
  PASTE_SIGN_COLOR(false),
  PASTE_SIGN_GLOWING(false),
  SEND_COPIED_MESSAGE(true),
  SEND_PASTED_MESSAGE(true),
  INK_SAC_AS_SHORTCUT(true),
  ;

  public static final List<SettingFlag> ALL_VALUES = List.of(values());

  public final boolean defaultEnabled;

  SettingFlag(boolean defaultEnabled) {
    this.defaultEnabled = defaultEnabled;
  }

  public static @Nullable SettingFlag byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }
}

package me.blvckbytes.bbtweaks.sign_copier.settings;

import java.util.List;

public enum SettingFlag {
  // NOTE: The ordinal of this enum is used as the main identifier!
  PASTE_SIGN_COLOR(false),
  PASTE_SIGN_GLOWING(false),
  SEND_COPIED_MESSAGE(true),
  SEND_PASTED_MESSAGE(true),
  INK_SAC_AS_SHORTCUT(true),
  PASTE_ADDITIONAL_ATTRIBUTES(false),
  COPY_FROM_BACK_SIDE(false),
  PASTE_TO_BACK_SIDE(false),
  ;

  public static final List<SettingFlag> ALL_VALUES = List.of(values());

  public final boolean defaultEnabled;

  SettingFlag(boolean defaultEnabled) {
    this.defaultEnabled = defaultEnabled;
  }
}

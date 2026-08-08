package me.blvckbytes.bbtweaks.emotions;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public enum NotificationPart {
  // NOTE: The ordinal of this enum is used as the main identifier!
  CHAT,
  ACTION_BAR,
  TITLE,
  SOUND,
  EFFECT,
  ;

  public static final List<NotificationPart> ALL_VALUES = Arrays.asList(values());

  public static @Nullable NotificationPart byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }
}

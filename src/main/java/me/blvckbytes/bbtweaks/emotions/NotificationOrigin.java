package me.blvckbytes.bbtweaks.emotions;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public enum NotificationOrigin {
  // NOTE: The ordinal of this enum is used as the main identifier!
  TARGETED_VIA_ALL,
  TARGETED_DIRECTLY,
  IS_SENDER,
  BROADCAST,
  ;

  public static final List<NotificationOrigin> ALL_VALUES = Arrays.asList(values());

  public static EnumSet<NotificationOrigin> makeDefaults(NotificationPart part) {
    return switch (part) {
      case CHAT -> EnumSet.allOf(NotificationOrigin.class);
      case ACTION_BAR -> EnumSet.noneOf(NotificationOrigin.class);
      case TITLE, EFFECT, SOUND -> EnumSet.of(TARGETED_VIA_ALL, TARGETED_DIRECTLY, IS_SENDER);
    };
  }

  public static @Nullable NotificationOrigin byOrdinalOrNull(int ordinal) {
    if (ordinal < 0 || ordinal >= ALL_VALUES.size())
      return null;

    return ALL_VALUES.get(ordinal);
  }
}

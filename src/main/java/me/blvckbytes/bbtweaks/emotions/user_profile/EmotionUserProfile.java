package me.blvckbytes.bbtweaks.emotions.user_profile;

import me.blvckbytes.bbtweaks.emotions.EmotionSection;
import me.blvckbytes.bbtweaks.emotions.NotificationPart;
import me.blvckbytes.bbtweaks.emotions.NotificationOrigin;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class EmotionUserProfile {

  public final Player player;

  public final Map<String, Long> lastUseStampByEmotionIdentifierLower;

  public final EnumMap<NotificationPart, EnumSet<NotificationOrigin>> enabledOriginsByPart;

  public EmotionUserProfile(Player player) {
    this.player = player;

    this.lastUseStampByEmotionIdentifierLower = new HashMap<>();
    this.enabledOriginsByPart = new EnumMap<>(NotificationPart.class);

    for (var part : NotificationPart.ALL_VALUES)
      getEnabledOriginsForPart(part);
  }

  public EnumSet<NotificationOrigin> getEnabledOriginsForPart(NotificationPart part) {
    // By default, all parts from all origins are received.
    return enabledOriginsByPart.computeIfAbsent(part, NotificationOrigin::makeDefaults);
  }

  public void toggleReceiving(NotificationPart part, NotificationOrigin origin) {
    var enabledOrigins = getEnabledOriginsForPart(part);

    if (enabledOrigins.contains(origin)) {
      enabledOrigins.remove(origin);
      return;
    }

    enabledOrigins.add(origin);
  }

  public boolean doesReceive(NotificationPart part, NotificationOrigin origin) {
    var enabledOrigins = enabledOriginsByPart.get(part);

    if (enabledOrigins == null)
      return true;

    return enabledOrigins.contains(origin);
  }

  public void touchCooldown(EmotionSection emotion) {
    if (emotion.hasCooldownBypassPermission(player))
      return;

    lastUseStampByEmotionIdentifierLower.put(emotion.identifierLower, System.currentTimeMillis());
  }

  public int getRemainingCooldownSeconds(EmotionSection emotion) {
    if (emotion.hasCooldownBypassPermission(player))
      return 0;

    var lastUseStamp = lastUseStampByEmotionIdentifierLower.get(emotion.identifierLower);

    if (lastUseStamp == null)
      return 0;

    var elapsedSeconds = (int) (System.currentTimeMillis() - lastUseStamp) / 1000;

    if (elapsedSeconds >= emotion.cooldownSeconds)
      return 0;

    return emotion.cooldownSeconds - elapsedSeconds;
  }
}

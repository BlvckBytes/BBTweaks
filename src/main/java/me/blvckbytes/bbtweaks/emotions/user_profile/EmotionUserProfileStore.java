package me.blvckbytes.bbtweaks.emotions.user_profile;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.emotions.NotificationOrigin;
import me.blvckbytes.bbtweaks.emotions.NotificationPart;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class EmotionUserProfileStore implements Disableable, Listener {

  private final Map<UUID, EmotionUserProfile> userProfileByPlayerId;

  private final NamespacedKey keyLastUseStamps;
  private final NamespacedKey keyEnabledOriginMasks;

  public EmotionUserProfileStore(
    Plugin plugin
  ) {
    this.userProfileByPlayerId = new HashMap<>();

    this.keyLastUseStamps = new NamespacedKey(plugin, "emotion-last-use-stamps");
    this.keyEnabledOriginMasks = new NamespacedKey(plugin, "emotion-enabled-origin-masks");
  }

  public EmotionUserProfile accessUserProfile(Player player) {
    return userProfileByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var preferences = userProfileByPlayerId.remove(event.getPlayer().getUniqueId());

    if (preferences != null)
      save(preferences);
  }

  @Override
  public void disable() {
    userProfileByPlayerId.values().forEach(this::save);
    userProfileByPlayerId.clear();
  }

  private EmotionUserProfile load(Player player) {
    var result = new EmotionUserProfile(player);
    var pdc = player.getPersistentDataContainer();

    var lastUseStampsCsv = pdc.get(keyLastUseStamps, PersistentDataType.STRING);

    if (lastUseStampsCsv != null) {
      for (var csvEntry : lastUseStampsCsv.split(";")) {
        var entryParts = csvEntry.split("=", 2);

        if (entryParts.length != 2)
          continue;

        long lastUseStamp;

        try {
          lastUseStamp = Long.parseLong(entryParts[1]);
        } catch (Throwable _) {
          continue;
        }

        result.lastUseStampByEmotionIdentifierLower.put(entryParts[0], lastUseStamp);
      }
    }

    var enabledOriginMasks = pdc.get(keyEnabledOriginMasks, PersistentDataType.INTEGER_ARRAY);

    if (enabledOriginMasks != null) {
      result.enabledOriginsByPart.clear();

      for (var index = 0; index < enabledOriginMasks.length; ++index) {
        var notificationPart = NotificationPart.byOrdinalOrNull(index);

        if (notificationPart == null)
          break;

        var enabledOrigins = result.enabledOriginsByPart.computeIfAbsent(notificationPart, _ -> EnumSet.noneOf(NotificationOrigin.class));

        var maskValue = enabledOriginMasks[index];

        for (var notificationOrigin : NotificationOrigin.ALL_VALUES) {
          if ((maskValue & (1 << notificationOrigin.ordinal())) == 0)
            continue;

          enabledOrigins.add(notificationOrigin);
        }
      }
    }

    return result;
  }

  private void save(EmotionUserProfile profile) {
    var pdc = profile.player.getPersistentDataContainer();

    var lastUseStampsCsv = new StringJoiner(";");

    for (var lastUseEntry : profile.lastUseStampByEmotionIdentifierLower.entrySet())
      lastUseStampsCsv.add(lastUseEntry.getKey() + "=" + lastUseEntry.getValue());

    pdc.set(keyLastUseStamps, PersistentDataType.STRING, lastUseStampsCsv.toString());

    var enabledOriginMasks = new IntArrayList();

    for (var notificationPart : NotificationPart.ALL_VALUES) {
      var enabledOrigins = profile.enabledOriginsByPart.get(notificationPart);

      var maskValue = 0;

      for (var enabledOrigin : enabledOrigins)
        maskValue |= 1 << enabledOrigin.ordinal();

      enabledOriginMasks.add(maskValue);
    }

    pdc.set(keyEnabledOriginMasks, PersistentDataType.INTEGER_ARRAY, enabledOriginMasks.toIntArray());
  }
}

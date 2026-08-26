package me.blvckbytes.bbtweaks.item_piling.preferences;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemPilingPreferencesStore implements Listener, Disableable {

  private final Map<UUID, ItemPilingPreferences> preferencesByPlayerId;

  private final NamespacedKey keyFlags;

  public ItemPilingPreferencesStore(
    Plugin plugin
  ) {
    this.preferencesByPlayerId = new HashMap<>();

    this.keyFlags = new NamespacedKey(plugin, "item-piling-flags");
  }

  public ItemPilingPreferences accessPreferences(Player player) {
    return preferencesByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var preferences = preferencesByPlayerId.remove(event.getPlayer().getUniqueId());

    if (preferences != null)
      save(preferences);
  }

  @Override
  public void disable() {
    preferencesByPlayerId.values().forEach(this::save);
    preferencesByPlayerId.clear();
  }

  private ItemPilingPreferences load(Player player) {
    var pdc = player.getPersistentDataContainer();
    var result = new ItemPilingPreferences(player);

    var flagsMaskValue = pdc.get(keyFlags, PersistentDataType.INTEGER);

    if (flagsMaskValue != null) {
      result.flags.clear();

      for (var flag : PreferenceFlag.ALL_VALUES) {
        if ((flagsMaskValue & (1 << flag.ordinal())) != 0)
          result.flags.add(flag);
      }
    }

    return result;
  }

  private void save(ItemPilingPreferences preferences) {
    var pdc = preferences.player.getPersistentDataContainer();

    var flagsMask = 0;

    for (var flag : PreferenceFlag.ALL_VALUES) {
      if (preferences.flags.contains(flag))
        flagsMask |= 1 << flag.ordinal();
    }

    pdc.set(keyFlags, PersistentDataType.INTEGER, flagsMask);
  }
}

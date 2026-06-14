package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoPickupContainerSettingsStore implements Listener, Disableable {

  private final NamespacedKey keyEnabled;

  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, AutoPickupContainerSettings> settingsByPlayerId;

  public AutoPickupContainerSettingsStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.keyEnabled = new NamespacedKey(plugin, "auto-pickup-container-enabled");

    this.config = config;

    this.settingsByPlayerId = new HashMap<>();
  }

  public AutoPickupContainerSettings accessSettings(Player player) {
    return settingsByPlayerId.computeIfAbsent(player.getUniqueId(), k -> load(player));
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    accessSettings(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var settings = settingsByPlayerId.remove(event.getPlayer().getUniqueId());

    if (settings != null)
      save(settings);
  }

  @Override
  public void disable() {
    settingsByPlayerId.values().forEach(this::save);
    settingsByPlayerId.clear();
  }

  private AutoPickupContainerSettings load(Player player) {
    var result = new AutoPickupContainerSettings(player, config);
    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      result.enabled = enabledValue;

    return result;
  }

  private void save(AutoPickupContainerSettings settings) {
    var pdc = settings.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, settings.enabled);
  }
}

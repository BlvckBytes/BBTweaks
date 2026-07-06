package me.blvckbytes.bbtweaks.hotbar_randomizer;

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

public class HotbarRandomizerSettingsStore implements Listener, Disableable {

  private final Map<UUID, HotbarRandomizerSettings> settingsByPlayerId;

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey keyEnabled;
  private final NamespacedKey keyEnabledSlotMask;

  public HotbarRandomizerSettingsStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.settingsByPlayerId = new HashMap<>();

    this.config = config;

    this.keyEnabled = new NamespacedKey(plugin, "hotbar-randomizer-enabled");
    this.keyEnabledSlotMask = new NamespacedKey(plugin, "hotbar-randomizer-enabled-slot-mask");
  }

  @Override
  public void disable() {
    settingsByPlayerId.values().forEach(this::save);
    settingsByPlayerId.clear();
  }

  public HotbarRandomizerSettings accessSettings(Player player) {
    return settingsByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
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

  private void save(HotbarRandomizerSettings settings) {
    var pdc = settings.player.getPersistentDataContainer();

    var enabledSlotMask = 0;

    for (var index = 0; index < HotbarRandomizerSettings.HOTBAR_SLOT_COUNT; ++index) {
      if (settings.getSlotEnableState(index))
        enabledSlotMask |= 1 << index;
    }

    pdc.set(keyEnabledSlotMask, PersistentDataType.INTEGER, enabledSlotMask);
    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, settings.enabled);
  }

  private HotbarRandomizerSettings load(Player player) {
    var settings = new HotbarRandomizerSettings(player, config);
    var pdc = player.getPersistentDataContainer();

    var enabledSlotMaskValue = pdc.get(keyEnabledSlotMask, PersistentDataType.INTEGER);

    if (enabledSlotMaskValue != null) {
      for (var index = 0; index < HotbarRandomizerSettings.HOTBAR_SLOT_COUNT; ++index) {
        if ((enabledSlotMaskValue & (1 << index)) != 0)
          settings.setSlotEnableState(index, true);
      }
    }

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      settings.enabled = enabledValue;

    return settings;
  }
}

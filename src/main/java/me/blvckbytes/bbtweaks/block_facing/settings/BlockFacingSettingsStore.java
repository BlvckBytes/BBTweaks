package me.blvckbytes.bbtweaks.block_facing.settings;

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

public class BlockFacingSettingsStore implements Listener, Disableable {

  private final Map<UUID, BlockFacingSettings> settingsByPlayerId;

  private final ConfigKeeper<MainSection> config;

  private final NamespacedKey keyEnabled, keyFacingOverride;

  public BlockFacingSettingsStore(
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.settingsByPlayerId = new HashMap<>();

    this.config = config;

    this.keyEnabled = new NamespacedKey(plugin, "block-facing-enabled");
    this.keyFacingOverride = new NamespacedKey(plugin, "block-facing-facing-override");
  }

  public BlockFacingSettings access(Player player) {
    return this.settingsByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  @Override
  public void disable() {
    settingsByPlayerId.values().forEach(this::save);
    settingsByPlayerId.clear();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    access(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var settings = settingsByPlayerId.remove(event.getPlayer().getUniqueId());

    if (settings != null)
      save(settings);
  }

  private void save(BlockFacingSettings settings) {
    var pdc = settings.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, settings.enabled);
    pdc.set(keyFacingOverride, PersistentDataType.INTEGER, settings.facingOverride.ordinal());
  }

  private BlockFacingSettings load(Player player) {
    var pdc = player.getPersistentDataContainer();
    var settings = new BlockFacingSettings(player, config);

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);

    if (enabledValue != null)
      settings.enabled = enabledValue;

    var facingOverrideValue = pdc.get(keyFacingOverride, PersistentDataType.INTEGER);

    if (facingOverrideValue != null) {
      var facingOverride = FacingOverride.byOrdinalOrNull(facingOverrideValue);

      if (facingOverride != null)
        settings.facingOverride = facingOverride;
    }

    return settings;
  }
}

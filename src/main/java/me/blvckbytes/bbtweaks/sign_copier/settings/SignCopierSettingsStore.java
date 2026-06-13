package me.blvckbytes.bbtweaks.sign_copier.settings;

import it.unimi.dsi.fastutil.ints.IntArrayList;
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

public class SignCopierSettingsStore implements Listener, Disableable {

  private final NamespacedKey keyEnabledFlags;

  private final Map<UUID, SignCopierSettings> settingsByPlayerId;

  public SignCopierSettingsStore(
    Plugin plugin
  ) {
    this.keyEnabledFlags = new NamespacedKey(plugin, "sign-copier-enabled-flags");

    this.settingsByPlayerId = new HashMap<>();
  }

  public SignCopierSettings accessSettings(Player player) {
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

  private SignCopierSettings load(Player player) {
    var settings = new SignCopierSettings(player);

    var pdc = player.getPersistentDataContainer();

    var enabledFlags = pdc.get(keyEnabledFlags, PersistentDataType.INTEGER_ARRAY);

    if (enabledFlags != null) {
      settings.flags.clear();

      for (var flagOrdinal : enabledFlags)
        settings.flags.add(SettingFlag.byOrdinalOrNull(flagOrdinal));
    }

    return settings;
  }

  private void save(SignCopierSettings settings) {
    var pdc = settings.player.getPersistentDataContainer();

    var enabledFlags = new IntArrayList();

    for (var flag : settings.flags)
      enabledFlags.add(flag.ordinal());

    pdc.set(keyEnabledFlags, PersistentDataType.INTEGER_ARRAY, enabledFlags.toIntArray());
  }
}

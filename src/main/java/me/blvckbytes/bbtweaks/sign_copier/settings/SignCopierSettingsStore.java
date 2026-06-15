package me.blvckbytes.bbtweaks.sign_copier.settings;

import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
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

    // Was an int[] of enabled ordinals previously, which doesn't allow for
    // falling back on default values, so the data-layout changed since.
    if (pdc.has(keyEnabledFlags, PersistentDataType.LONG)) {
      var enabledValue = pdc.get(keyEnabledFlags, PersistentDataType.LONG);

      assert enabledValue != null;

      var flagCount = IntTuple.getFirst(enabledValue);
      var enabledMask = IntTuple.getSecond(enabledValue);

      settings.flags.clear();

      for (var flag : SettingFlag.ALL_VALUES) {
        if (flag.ordinal() >= flagCount) {
          if (flag.defaultEnabled)
            settings.flags.add(flag);

          continue;
        }

        if ((enabledMask & (1 << flag.ordinal())) != 0)
          settings.flags.add(flag);
      }
    }

    return settings;
  }

  private void save(SignCopierSettings settings) {
    var pdc = settings.player.getPersistentDataContainer();

    var flagCount = SettingFlag.ALL_VALUES.size();
    var enabledMask = 0;

    for (var flag : settings.flags)
      enabledMask |= 1 << flag.ordinal();

    pdc.set(keyEnabledFlags, PersistentDataType.LONG, IntTuple.create(flagCount, enabledMask));
  }
}

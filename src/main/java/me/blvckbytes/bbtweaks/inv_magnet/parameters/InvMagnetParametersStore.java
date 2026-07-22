package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
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

import java.util.*;

public class InvMagnetParametersStore implements Disableable, Listener {

  private final Plugin plugin;

  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, InvMagnetParameters> parametersByPlayerId;

  private final Map<String, NamespacedKey> keyEnabledByWorldGroupIdentifyingName;
  private final Map<String, NamespacedKey> keyRadiusByWorldGroupIdentifyingName;

  public InvMagnetParametersStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.keyEnabledByWorldGroupIdentifyingName = new HashMap<>();
    this.keyRadiusByWorldGroupIdentifyingName = new HashMap<>();

    this.plugin = plugin;
    this.config = config;
    this.parametersByPlayerId = new HashMap<>();

    makeNamespacedKeys();
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    makeNamespacedKeys();

    for (var parameter : parametersByPlayerId.values())
      parameter.updateLimitsAndConstrain();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    parametersByPlayerId.put(player.getUniqueId(), load(player));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var player = event.getPlayer();
    var parameters = parametersByPlayerId.remove(player.getUniqueId());

    if (parameters != null)
      save(parameters);
  }

  @Override
  public void disable() {
    parametersByPlayerId.values().forEach(this::save);
    parametersByPlayerId.clear();
  }

  public InvMagnetParameters accessParameters(Player player) {
    return parametersByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> load(player));
  }

  private InvMagnetParameters load(Player player) {
    var result = new InvMagnetParameters(player, config);

    var pdc = player.getPersistentDataContainer();

    for (var enabledEntry : keyEnabledByWorldGroupIdentifyingName.entrySet()) {
      var enabledValue = pdc.get(enabledEntry.getValue(), PersistentDataType.BOOLEAN);

      if (enabledValue != null)
        result.enabledByWorldGroupIdentifyingName.put(enabledEntry.getKey(), (boolean) enabledValue);
    }

    for (var radiusEntry : keyRadiusByWorldGroupIdentifyingName.entrySet()) {
      var radiusValue = pdc.get(radiusEntry.getValue(), PersistentDataType.INTEGER);

      if (radiusValue != null)
        result.radiusByWorldGroupIdentifyingName.put(radiusEntry.getKey(), (int) radiusValue);
    }

    return result;
  }

  private void save(InvMagnetParameters parameters) {
    var pdc = parameters.player.getPersistentDataContainer();

    for (var enabledEntry : parameters.enabledByWorldGroupIdentifyingName.object2BooleanEntrySet()) {
      var keyEnabled = keyEnabledByWorldGroupIdentifyingName.get(enabledEntry.getKey());

      if (keyEnabled != null)
        pdc.set(keyEnabled, PersistentDataType.BOOLEAN, enabledEntry.getBooleanValue());
    }

    for (var radiusEntry : parameters.radiusByWorldGroupIdentifyingName.object2IntEntrySet()) {
      var keyRadius = keyRadiusByWorldGroupIdentifyingName.get(radiusEntry.getKey());

      if (keyRadius != null)
        pdc.set(keyRadius, PersistentDataType.INTEGER, radiusEntry.getIntValue());
    }
  }

  private void makeNamespacedKeys() {
    for (var worldGroup : config.rootSection.invMagnet.worldGroups) {
      var name = worldGroup.identifyingName;
      var suffix = "-" + name;

      if (name.equalsIgnoreCase(config.rootSection.invMagnet.preExistingWorldGroupName))
        suffix = "";

      keyEnabledByWorldGroupIdentifyingName.put(name, new NamespacedKey(plugin, "inv-magnet-enabled" + suffix));
      keyRadiusByWorldGroupIdentifyingName.put(name, new NamespacedKey(plugin, "inv-magnet-radius" + suffix));
    }
  }
}

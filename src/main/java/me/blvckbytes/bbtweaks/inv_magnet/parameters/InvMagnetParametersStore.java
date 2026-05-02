package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
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

public class InvMagnetParametersStore implements Listener {

  private final NamespacedKey keyEnabled, keyRadius;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, InvMagnetParameters> parametersByPlayerId;

  public InvMagnetParametersStore(Plugin plugin, ConfigKeeper<MainSection> config) {
    keyEnabled = new NamespacedKey(plugin, "inv-magnet-enabled");
    keyRadius = new NamespacedKey(plugin, "inv-magnet-radius");

    this.config = config;
    this.parametersByPlayerId = new HashMap<>();
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

  public void onShutdown() {
    parametersByPlayerId.values().forEach(this::save);
    parametersByPlayerId.clear();
  }

  public InvMagnetParameters accessParameters(Player player) {
    return parametersByPlayerId.computeIfAbsent(player.getUniqueId(), k -> load(player));
  }

  private InvMagnetParameters load(Player player) {
    var result = new InvMagnetParameters(player, config);

    var pdc = player.getPersistentDataContainer();

    var enabledValue = pdc.get(keyEnabled, PersistentDataType.BOOLEAN);
    result.enabled = enabledValue != null && enabledValue;

    var radiusValue = pdc.get(keyRadius, PersistentDataType.INTEGER);
    result.setRadiusAndGetIfExceeded(radiusValue == null ? result.getLimits().maxRadius() : radiusValue);

    return result;
  }

  private void save(InvMagnetParameters parameters) {
    var pdc = parameters.player.getPersistentDataContainer();

    pdc.set(keyEnabled, PersistentDataType.BOOLEAN, parameters.enabled);
    pdc.set(keyRadius, PersistentDataType.INTEGER, parameters.getRadius());
  }
}

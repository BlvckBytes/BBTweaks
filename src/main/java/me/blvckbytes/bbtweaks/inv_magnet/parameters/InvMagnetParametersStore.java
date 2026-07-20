package me.blvckbytes.bbtweaks.inv_magnet.parameters;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class InvMagnetParametersStore implements Disableable, Listener {

  private final NamespacedKey keyEnabled, keyRadius;
  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  // TODO: Scope per config-definable world-group, where each group has a permission.
  private final Map<UUID, InvMagnetParameters> parametersByPlayerId;

  private final List<World> worlds;

  public InvMagnetParametersStore(
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    keyEnabled = new NamespacedKey(plugin, "inv-magnet-enabled");
    keyRadius = new NamespacedKey(plugin, "inv-magnet-radius");

    this.plugin = plugin;
    this.config = config;
    this.parametersByPlayerId = new HashMap<>();

    this.worlds = new ArrayList<>();

    loadWorldsFromConfig();
  }

  public List<World> getAllowedWorlds() {
    return Collections.unmodifiableList(worlds);
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper == config)
      loadWorldsFromConfig();
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

  private void loadWorldsFromConfig() {
    worlds.clear();

    for (var worldName : config.rootSection.invMagnet.worlds) {
      var world = Bukkit.getWorld(worldName);

      if (world == null) {
        plugin.getLogger().warning("Could not find world for item-magnet called \"" + worldName + "\"");
        continue;
      }

      worlds.add(world);
    }
  }
}

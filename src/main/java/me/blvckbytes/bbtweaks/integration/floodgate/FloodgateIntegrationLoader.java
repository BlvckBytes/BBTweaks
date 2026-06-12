package me.blvckbytes.bbtweaks.integration.floodgate;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

public class FloodgateIntegrationLoader {

  @WrappedDependency
  public final FloodgateIntegration floodgateIntegration;

  public FloodgateIntegrationLoader(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
      floodgateIntegration = player -> false;
      return;
    }

    plugin.getLogger().info("Integrating with floodgate as to detect floodgate-players");

    floodgateIntegration = player -> FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
  }
}

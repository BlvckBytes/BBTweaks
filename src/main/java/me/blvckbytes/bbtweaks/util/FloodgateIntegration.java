package me.blvckbytes.bbtweaks.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.logging.Logger;

public interface FloodgateIntegration {

  boolean isFloodgatePlayer(Player player);

  static FloodgateIntegration load(Logger logger) {
    if (!Bukkit.getPluginManager().isPluginEnabled("floodgate"))
      return player -> false;

    logger.info("Integrating with floodgate as to detect floodgate-players");

    return player -> FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
  }
}

package me.blvckbytes.bbtweaks.integration.lwc;

import me.blvckbytes.bbtweaks.auto_wirer.AutoWirer;
import org.bukkit.Bukkit;

public class LWCIntegrationLoader {

  public LWCIntegrationLoader(
    AutoWirer wirer
  ) throws Throwable {
    if (!Bukkit.getPluginManager().isPluginEnabled("LWC"))
      return;

    wirer.withSingleton(LWCIntegrationHandler.class);
  }
}

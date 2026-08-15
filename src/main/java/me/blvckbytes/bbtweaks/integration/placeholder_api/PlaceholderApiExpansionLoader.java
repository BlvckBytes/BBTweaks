package me.blvckbytes.bbtweaks.integration.placeholder_api;

import me.blvckbytes.bbtweaks.auto_wirer.AutoWirer;
import org.bukkit.Bukkit;

public class PlaceholderApiExpansionLoader {

  public PlaceholderApiExpansionLoader(AutoWirer wirer) throws Throwable {
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
      wirer.withSingleton(PlaceholderApiExpansion.class);
  }
}

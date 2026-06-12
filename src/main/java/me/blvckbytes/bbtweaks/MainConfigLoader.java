package me.blvckbytes.bbtweaks;

import at.blvckbytes.cm_mapper.ConfigHandler;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.plugin.Plugin;

public class MainConfigLoader {

  @WrappedDependency
  public final ConfigKeeper<MainSection> config;

  public MainConfigLoader(Plugin plugin) throws Exception {
    var configHandler = new ConfigHandler(plugin, "config");
    config = new ConfigKeeper<>(configHandler, "config.yml", MainSection.class);
  }
}

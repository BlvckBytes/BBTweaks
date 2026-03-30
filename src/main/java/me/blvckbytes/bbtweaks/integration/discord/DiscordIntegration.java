package me.blvckbytes.bbtweaks.integration.discord;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public class DiscordIntegration {

  private static @Nullable DiscordIntegration instance;

  private final @Nullable DiscordApi discordApi;

  private DiscordIntegration(Plugin plugin, Logger logger, ConfigKeeper<MainSection> config) {
    Plugin dependency;

    if ((dependency = Bukkit.getPluginManager().getPlugin("EssentialsDiscord")) == null || !dependency.isEnabled()) {
      logger.info("Could not locate a loaded instance of the EssentialsDiscord-plugin; not hooking into Discord!");
      discordApi = null;
      return;
    }

    var discordApi = new EssentialsDiscordApi(plugin, logger, config);

    config.registerReloadListener(discordApi::onConfigReload);

    this.discordApi = discordApi;
  }

  public @Nullable DiscordApi getDiscordApi() {
    return discordApi;
  }

  public static DiscordIntegration getOrLoadInstance(Plugin plugin, Logger logger, ConfigKeeper<MainSection> config) {
    if (instance == null)
      instance = new DiscordIntegration(plugin, logger, config);

    return instance;
  }
}

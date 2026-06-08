package me.blvckbytes.bbtweaks.integration.discord;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class DiscordIntegration {

  private final @Nullable DiscordApi discordApi;

  public DiscordIntegration(Plugin plugin, ConfigKeeper<MainSection> config) {
    Plugin dependency;

    if ((dependency = Bukkit.getPluginManager().getPlugin("EssentialsDiscord")) == null || !dependency.isEnabled()) {
      plugin.getLogger().info("Could not locate a loaded instance of the EssentialsDiscord-plugin; not hooking into Discord!");
      discordApi = null;
      return;
    }

    var discordApi = new EssentialsDiscordApi(plugin, config);

    config.registerReloadListener(discordApi::onConfigReload);

    this.discordApi = discordApi;
  }

  public @Nullable DiscordApi getDiscordApi() {
    return discordApi;
  }
}

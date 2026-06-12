package me.blvckbytes.bbtweaks.integration.discord;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class DiscordIntegrationLoader {

  private static final DiscordIntegration STUBBED_DISCORD_INTEGRATION = message -> {};

  @WrappedDependency
  public final DiscordIntegration discordIntegration;

  public DiscordIntegrationLoader(Plugin plugin, ConfigKeeper<MainSection> config) {
    Plugin dependency;

    if ((dependency = Bukkit.getPluginManager().getPlugin("EssentialsDiscord")) == null || !dependency.isEnabled()) {
      plugin.getLogger().info("Could not locate a loaded instance of the EssentialsDiscord-plugin; not hooking into Discord!");
      discordIntegration = STUBBED_DISCORD_INTEGRATION;
      return;
    }

    this.discordIntegration = new EssentialsDiscordIntegration(plugin, config);
    plugin.getLogger().info("Hooked into the EssentialsDiscord-plugin!");
  }
}

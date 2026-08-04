package me.blvckbytes.bbtweaks.integration.mc_mmo;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class McMMOIntegrationLoader {

  private static final McMMOIntegration STUBBED_MCMMO_INTEGRATION = new McMMOIntegration() {

    @Override
    public int applySmeltingRecipeExpBoost(Player player, int experience) {
      return 0;
    }

    @Override
    public @Nullable Object getSpecificsApi() {
      return null;
    }
  };


  @WrappedDependency
  public final McMMOIntegration mcMMOIntegration;

  public McMMOIntegrationLoader(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
      mcMMOIntegration = STUBBED_MCMMO_INTEGRATION;
      plugin.getLogger().warning("Could not integrate with mcMMO, as the plugin is not loaded");
      return;
    }

    mcMMOIntegration = new McMMOIntegrationImpl();
    plugin.getLogger().info("Integrated with mcMMO");
  }
}

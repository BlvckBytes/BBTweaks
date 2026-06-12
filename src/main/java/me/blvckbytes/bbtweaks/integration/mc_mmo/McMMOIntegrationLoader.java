package me.blvckbytes.bbtweaks.integration.mc_mmo;

import com.gmail.nossr50.util.player.UserManager;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class McMMOIntegrationLoader {

  @WrappedDependency
  public final McMMOIntegration mcMMOIntegration;

  public McMMOIntegrationLoader(Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("mcMMO")) {
      plugin.getLogger().warning("Could not integrate with mcMMO, as the plugin is not loaded");
      mcMMOIntegration = (player, experience) -> experience;
      return;
    }

    plugin.getLogger().info("Integrated with mcMMO as to boost XP");

    mcMMOIntegration =((player, experience) -> {
      var internalPlayer = UserManager.getPlayer(player);

      if (internalPlayer == null)
        return experience;

      return internalPlayer.getSmeltingManager().vanillaXPBoost(experience);
    });
  }
}

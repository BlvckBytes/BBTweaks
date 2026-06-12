package me.blvckbytes.bbtweaks.integration.arm;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ArmIntegrationLoader {

  private static final ArmIntegration STUBBED_ARM_INTEGRATION = new ArmIntegration() {

    @Override
    public long getRemainingShopRegionTime(Player player) {
      return 0;
    }

    @Override
    public long getRemainingCreativeRegionTime(Player player) {
      return 0;
    }
  };

  @WrappedDependency
  public final ArmIntegration armIntegration;

  public ArmIntegrationLoader(ConfigKeeper<MainSection> config, Plugin plugin) {
    if (!Bukkit.getPluginManager().isPluginEnabled("AdvancedRegionMarket")) {
      armIntegration = STUBBED_ARM_INTEGRATION;
      return;
    }

    armIntegration = new ArmIntegrationImpl(config);
    plugin.getLogger().info("Loaded sidebar-related ARMIntegration");
  }
}

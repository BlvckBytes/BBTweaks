package me.blvckbytes.bbtweaks.integration.temp_fly;

import me.blvckbytes.bbtweaks.auto_wirer.WrappedDependency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TempFlyIntegrationLoader {

  private static final TempFlyIntegration STUBBED_TEMP_FLY_INTEGRATION = new TempFlyIntegration() {

    @Override
    public int getRemainingTimeSeconds(Player player) {
      return 0;
    }
  };

  @WrappedDependency
  public TempFlyIntegration tempFlyIntegration;

  public TempFlyIntegrationLoader() {
    if (!Bukkit.getPluginManager().isPluginEnabled("TempFly")) {
      this.tempFlyIntegration = STUBBED_TEMP_FLY_INTEGRATION;
      return;
    }

    this.tempFlyIntegration = new TempFlyIntegrationImpl();
  }
}

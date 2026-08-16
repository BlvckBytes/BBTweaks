package me.blvckbytes.bbtweaks.integration.temp_fly;

import com.moneybags.tempfly.TempFly;
import com.moneybags.tempfly.TempFlyAPI;
import org.bukkit.entity.Player;

public class TempFlyIntegrationImpl implements TempFlyIntegration {

  private final TempFlyAPI api;

  public TempFlyIntegrationImpl() {
    this.api = TempFly.getAPI();
  }

  @Override
  public int getRemainingTimeSeconds(Player player) {
    return (int) api.getFlightTime(player.getUniqueId());
  }
}

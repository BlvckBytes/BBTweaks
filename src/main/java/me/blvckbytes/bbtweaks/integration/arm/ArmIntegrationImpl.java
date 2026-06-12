package me.blvckbytes.bbtweaks.integration.arm;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.CountdownRegion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ArmIntegrationImpl implements ArmIntegration {

  private final ConfigKeeper<MainSection> config;
  private final AdvancedRegionMarket arm;

  public ArmIntegrationImpl(ConfigKeeper<MainSection> config) {
    this.config = config;
    this.arm = AdvancedRegionMarket.getInstance();

    if (arm == null || !arm.isEnabled())
      throw new IllegalStateException("Expected ARM to be loaded and enabled!");
  }

  @Override
  public long getRemainingShopRegionTime(Player player) {
    return getRemainingRegionTime(player, config.rootSection.sidebar.armIntegration.shopRegion);
  }

  @Override
  public long getRemainingCreativeRegionTime(Player player) {
    return getRemainingRegionTime(player, config.rootSection.sidebar.armIntegration.creativeRegion);
  }

  private long getRemainingRegionTime(Player player, ArmRegionSection regionSection) {
    var targetRegion = firstOwnedCountdownRegionOrNull(player, region -> (
      regionSection.matches(region.getRegionworld(), region.getRegion().getId())
    ));

    if (targetRegion == null)
      return 0;

    var remainingTime = targetRegion.getPayedTill() - System.currentTimeMillis();

    if (remainingTime < 0)
      return 0;

    return remainingTime;
  }

  private @Nullable CountdownRegion firstOwnedCountdownRegionOrNull(Player player, Predicate<CountdownRegion> predicate) {
    var playerRegions = arm.getRegionManager().getRegionsByOwner(player.getUniqueId());

    for (var region : playerRegions) {
      if (!(region instanceof CountdownRegion countdownRegion))
        continue;

      if (predicate.test(countdownRegion))
        return countdownRegion;
    }

    return null;
  }
}

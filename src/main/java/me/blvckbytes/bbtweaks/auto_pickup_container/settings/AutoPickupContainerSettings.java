package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AutoPickupContainerSettings extends ItemAttemptsKeeper {

  private static final boolean DEFAULT_ENABLED = true;

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;

  public AutoPickupContainerSettings(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    this.enabled = DEFAULT_ENABLED;
  }

  public void setEnabled(@Nullable Boolean value) {
    var newValue = value == null ? !enabled : value;

    if (enabled == newValue) {
      if (enabled) {
        config.rootSection.autoPickupContainer.command.functionalityAlreadyEnabled.sendMessage(player);
        return;
      }

      config.rootSection.autoPickupContainer.command.functionalityAlreadyDisabled.sendMessage(player);
      return;
    }

    enabled = newValue;

    if (enabled) {
      config.rootSection.autoPickupContainer.command.functionalityNowEnabled.sendMessage(player);
      return;
    }

    config.rootSection.autoPickupContainer.command.functionalityNowDisabled.sendMessage(player);
  }
}

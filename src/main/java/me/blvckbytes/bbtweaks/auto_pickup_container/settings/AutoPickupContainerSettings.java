package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.command.CapacityWarningMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AutoPickupContainerSettings extends ItemAttemptsKeeper {

  private static final boolean DEFAULT_ENABLED = true;

  public final Player player;
  private final ConfigKeeper<MainSection> config;

  public boolean enabled;
  public CapacityWarningMode capacityWarningMode;

  public AutoPickupContainerSettings(Player player, ConfigKeeper<MainSection> config) {
    this.player = player;
    this.config = config;

    this.enabled = DEFAULT_ENABLED;
    this.capacityWarningMode = CapacityWarningMode.DEFAULT_VALUE;
  }

  public void selectCapacityWarningMode(CapacityWarningMode capacityWarningMode) {
    var environment = new InterpretationEnvironment()
      .withVariable("mode", CapacityWarningMode.matcher.getNormalizedName(capacityWarningMode));

    if (this.capacityWarningMode == capacityWarningMode) {
      config.rootSection.autoPickupContainer.command.capacityWarningAlreadySelected.sendMessage(player, environment);
      return;
    }

    this.capacityWarningMode = capacityWarningMode;

    config.rootSection.autoPickupContainer.command.capacityWarningNowSelected.sendMessage(player, environment);
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

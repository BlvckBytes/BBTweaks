package me.blvckbytes.bbtweaks.block_facing.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.block_facing.settings.BlockFacingSettings;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class BlockFacingSettingsDisplayHandler extends DisplayHandler<BlockFacingSettingsDisplay, BlockFacingSettings> {

  private final FloodgateIntegration floodgateIntegration;

  public BlockFacingSettingsDisplayHandler(
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public BlockFacingSettingsDisplay instantiateDisplay(Player player, BlockFacingSettings displayData) {
    return new BlockFacingSettingsDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, BlockFacingSettingsDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    if (config.rootSection.blockFacing.settingsDisplay.items.enabled.getDisplaySlots().contains(slot)) {
      display.displayData.setEnabled(null);
      display.renderItems();
      return;
    }

    var facingOverride = display.determineFacingOverrideBySlot(slot);

    if (facingOverride == null)
      return;

    display.displayData.setFacingOverride(facingOverride);
    display.renderItems();
  }
}

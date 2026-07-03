package me.blvckbytes.bbtweaks.block_facing.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.block_facing.settings.BlockFacingSettings;
import me.blvckbytes.bbtweaks.block_facing.settings.FacingOverride;
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

    if (config.rootSection.blockFacing.settingsDisplay.items.modifyPlacedBlocks.getDisplaySlots().contains(slot)) {
      display.displayData.setModifyPlacedBlocks(null);
      display.renderItems();
      return;
    }

    if (config.rootSection.blockFacing.settingsDisplay.items.modifyExistingBlocks.getDisplaySlots().contains(slot)) {
      display.displayData.setModifyExistingBlocks(null);
      display.renderItems();
      return;
    }

    var facingOverride = display.determineFacingOverrideBySlot(slot);

    if (facingOverride == null)
      return;

    if (facingOverride == display.displayData.facingOverride) {
      config.rootSection.blockFacing.facingOverrideAlreadySelected.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("facing", FacingOverride.matcher.getNormalizedName(facingOverride))
      );

      return;
    }

    display.displayData.facingOverride = facingOverride;
    display.renderItems();

    config.rootSection.blockFacing.facingOverrideNowSelected.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("facing", FacingOverride.matcher.getNormalizedName(facingOverride))
    );
  }
}

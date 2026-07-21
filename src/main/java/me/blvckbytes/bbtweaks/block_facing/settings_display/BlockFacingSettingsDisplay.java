package me.blvckbytes.bbtweaks.block_facing.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.block_facing.settings.BlockFacingSettings;
import me.blvckbytes.bbtweaks.block_facing.settings.FacingOverride;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class BlockFacingSettingsDisplay extends Display<BlockFacingSettings> {

  public final boolean isFloodgate;

  public BlockFacingSettingsDisplay(
    Player player,
    BlockFacingSettings displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);
  }

  @Override
  protected void renderItems() {
    var environment = createEnvironment();

    config.rootSection.blockFacing.settingsDisplay.items.filler.renderInto(inventory, environment);

    config.rootSection.blockFacing.settingsDisplay.items.enabled.renderInto(inventory, environment);

    var facingItem = config.rootSection.blockFacing.settingsDisplay.items.facing;

    for (var slot : facingItem.getDisplaySlots()) {
      var facingOverride = determineFacingOverrideBySlot(slot);

      if (facingOverride == null)
        continue;

      var itemStack = facingItem.build(
        environment
          .withVariable("name", FacingOverride.matcher.getNormalizedName(facingOverride))
          .withVariable("facing_enabled", facingOverride == displayData.facingOverride)
      );

      inventory.setItem(slot, itemStack);
    }
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.blockFacing.settingsDisplay, createEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  public @Nullable FacingOverride determineFacingOverrideBySlot(int slot) {
    return FacingOverride.byOrdinalOrNull(
      config.rootSection.blockFacing.settingsDisplay.items.facing.getDisplaySlots().indexOf(slot)
    );
  }

  private InterpretationEnvironment createEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("enabled", displayData.enabled)
      .withVariable("is_floodgate", isFloodgate);
  }
}

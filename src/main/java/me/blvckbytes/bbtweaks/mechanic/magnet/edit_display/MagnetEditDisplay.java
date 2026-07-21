package me.blvckbytes.bbtweaks.mechanic.magnet.edit_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.EditSession;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class MagnetEditDisplay extends Display<EditSession> {

  public final boolean isFloodgate;

  public MagnetEditDisplay(
    Player player,
    EditSession displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);
  }

  @Override
  protected void renderItems() {
    var environment = createEnvironment();

    // Render filler first, such that it may be overridden by conditionally displayed items
    config.rootSection.mechanic.magnet.editDisplay.items.filler.renderInto(inventory, environment);

    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentX.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentY.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentZ.renderInto(inventory, environment);

    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetX.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetY.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetZ.renderInto(inventory, environment);

    config.rootSection.mechanic.magnet.editDisplay.items.save.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.cancel.renderInto(inventory, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.toggleClickDetection.renderInto(inventory, environment);
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.mechanic.magnet.editDisplay, createEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment createEnvironment() {
    return displayData.makeEnvironment()
      .withVariable("is_floodgate", isFloodgate);
  }
}

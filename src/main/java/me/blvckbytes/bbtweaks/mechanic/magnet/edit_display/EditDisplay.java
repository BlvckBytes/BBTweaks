package me.blvckbytes.bbtweaks.mechanic.magnet.edit_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.EditSession;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class EditDisplay extends Display<EditSession> {

  private final FloodgateIntegration floodgateIntegration;

  public EditDisplay(
    Player player,
    EditSession displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.floodgateIntegration = floodgateIntegration;

    show();
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
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.mechanic.magnet.editDisplay.createInventory(createEnvironment());
  }

  @Override
  public void onConfigReload() {}

  private InterpretationEnvironment createEnvironment() {
    var environment = new InterpretationEnvironment()
      .withVariable("magnet_x", displayData.parameters.sign.getX())
      .withVariable("magnet_y", displayData.parameters.sign.getY())
      .withVariable("magnet_z", displayData.parameters.sign.getZ())
      .withVariable("current_parameter", displayData.getCurrentParameter().name)
      .withVariable("is_floodgate", floodgateIntegration.isFloodgatePlayer(player));

    displayData.parameters.forEach(parameter -> {
      var variableName = parameter.name.toLowerCase();

      environment
        .withVariable(variableName, parameter.getValue())
        .withVariable(variableName + "_did_exceed_limit", parameter.didLastSetExceedLimit());
    });

    return environment;
  }
}

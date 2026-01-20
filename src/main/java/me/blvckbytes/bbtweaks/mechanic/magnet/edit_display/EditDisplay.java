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

  public final boolean isFloodgate;

  public EditDisplay(
    Player player,
    EditSession displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

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
    config.rootSection.mechanic.magnet.editDisplay.items.toggleClickDetection.renderInto(inventory, environment);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.mechanic.magnet.editDisplay.createInventory(createEnvironment());
  }

  @Override
  public void onConfigReload() {}

  private InterpretationEnvironment createEnvironment() {
    return displayData.makeEnvironment()
      .withVariable("is_floodgate", isFloodgate);
  }
}

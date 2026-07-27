package me.blvckbytes.bbtweaks.mechanic.magnet.edit_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.EditSession;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class MagnetEditDisplay extends Display<EditSession> {

  public MagnetEditDisplay(
    Player player,
    EditSession displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = createEnvironment();

    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentX.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentY.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentZ.renderInto(itemConsumer, environment);

    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetX.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetY.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetZ.renderInto(itemConsumer, environment);

    config.rootSection.mechanic.magnet.editDisplay.items.save.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.cancel.renderInto(itemConsumer, environment);
    config.rootSection.mechanic.magnet.editDisplay.items.toggleClickDetection.renderInto(itemConsumer, environment);
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.mechanic.magnet.editDisplay.items.filler;
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

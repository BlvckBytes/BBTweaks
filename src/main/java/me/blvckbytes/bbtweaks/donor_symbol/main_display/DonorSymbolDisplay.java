package me.blvckbytes.bbtweaks.donor_symbol.main_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfile;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class DonorSymbolDisplay extends Display<DonorSymbolProfile> {

  public DonorSymbolDisplay(
    Player player,
    DonorSymbolProfile displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.donorSymbol.mainDisplay.items.enabled.renderInto(itemConsumer, environment);

    config.rootSection.donorSymbol.mainDisplay.items.symbol.renderInto(
      itemConsumer,
      displayData.symbol.makeEnvironment()
        .inheritFrom(environment, false)
        .inheritFrom(displayData.color.makeEnvironment(), false)
    );

    config.rootSection.donorSymbol.mainDisplay.items.color.renderInto(
      itemConsumer,
      displayData.color.makeEnvironment()
        .inheritFrom(environment, false)
        .inheritFrom(displayData.symbol.makeEnvironment(), false)
    );

    config.rootSection.donorSymbol.mainDisplay.items.info.renderInto(itemConsumer, environment);
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.donorSymbol.mainDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.donorSymbol.mainDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("enabled", displayData.enabled)
      .withVariable("is_editing_other", displayData.player != player)
      .withVariable("profile_name", displayData.player.getName());
  }
}

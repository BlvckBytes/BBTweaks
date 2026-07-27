package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class ShowcaseDisplayHandler extends DisplayHandler<ShowcaseDisplay, ShowcaseDisplayData> {

  private final FloodgateIntegration floodgateIntegration;

  public ShowcaseDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, ShowcaseDisplay.class);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected ShowcaseDisplay instantiateDisplay(Player player, ShowcaseDisplayData displayData) {
    return new ShowcaseDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, ShowcaseDisplay display, ClickType clickType, int slot) {
    config.rootSection.mechanic.showcase.cannotModifyInventory.sendMessage(player);
  }
}

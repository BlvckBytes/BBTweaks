package me.blvckbytes.bbtweaks.inv_filter.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class InvFilterDisplayHandler extends DisplayHandler<InvFilterDisplay, InvFilterDisplayData> {

  private final FloodgateIntegration floodgateIntegration;
  private final IPPIntegration ippIntegration;

  public InvFilterDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    FloodgateIntegration floodgateIntegration,
    IPPIntegration ippIntegration
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
    this.ippIntegration = ippIntegration;
  }

  @Override
  public InvFilterDisplay instantiateDisplay(Player player, InvFilterDisplayData displayData) {
    return new InvFilterDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, InvFilterDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.invFilter.display.items.enabled.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.profile().setEnabledAndMessage(null);
      display.renderItems();
      return;
    }

    var filterSlotIndex = config.rootSection.invFilter.display.items.filterSlot.getDisplaySlots().indexOf(slot);

    if (filterSlotIndex < 0)
      return;

    if (clickType == ClickType.LEFT) {
      display.displayData.profile().setSelectedSlotIndexAndMessage(filterSlotIndex);
      display.renderItems();
      return;
    }

    if (clickType == ClickType.DROP) {
      display.displayData.profile().removeCurrentFilterIfSetAndMessage(ippIntegration, display.displayData.commandLabel());
      display.renderItems();
    }
  }
}

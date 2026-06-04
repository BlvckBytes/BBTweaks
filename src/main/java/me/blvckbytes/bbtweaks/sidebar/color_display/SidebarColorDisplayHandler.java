package me.blvckbytes.bbtweaks.sidebar.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class SidebarColorDisplayHandler extends DisplayHandler<SidebarColorDisplay, ColorDisplayData> {

  private final FloodgateIntegration floodgateIntegration;

  public SidebarColorDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
  }

  public SidebarColorDisplay instantiateDisplay(Player player, ColorDisplayData displayData) {
    return new SidebarColorDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarColorDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.sidebar.colorDisplay.items.backButton.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.backHandler().run();
      return;
    }

    if (config.rootSection.sidebar.colorDisplay.items.labelColorMode.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.selectingLabelColor = true;
      display.renderItems();
      return;
    }

    if (config.rootSection.sidebar.colorDisplay.items.valueColorMode.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.selectingLabelColor = false;
      display.renderItems();
      return;
    }

    var color = display.getColorBySlotIndex(slot);

    if (color != null)
      display.onColorSelection(color);
  }
}

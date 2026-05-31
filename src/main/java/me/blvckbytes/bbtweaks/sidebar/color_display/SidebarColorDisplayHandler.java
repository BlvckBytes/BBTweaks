package me.blvckbytes.bbtweaks.sidebar.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class SidebarColorDisplayHandler extends DisplayHandler<SidebarColorDisplay, ColorDisplayCallback> {

  private final FloodgateIntegration floodgateIntegration;

  public SidebarColorDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
  }

  public SidebarColorDisplay instantiateDisplay(Player player, ColorDisplayCallback displayData) {
    return new SidebarColorDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarColorDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.sidebar.colorDisplay.items.backButton.getDisplaySlots().contains(slot)) {
      display.displayData.onColorSelect(null);
      return;
    }

    var color = display.getColorBySlotIndex(slot);

    if (color != null)
      display.displayData.onColorSelect(color);
  }
}

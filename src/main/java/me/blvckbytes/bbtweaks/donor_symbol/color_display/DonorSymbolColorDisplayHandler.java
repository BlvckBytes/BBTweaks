package me.blvckbytes.bbtweaks.donor_symbol.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class DonorSymbolColorDisplayHandler extends DisplayHandler<DonorSymbolColorDisplay, ColorSelectionData> {

  private final FloodgateIntegration floodgateIntegration;

  public DonorSymbolColorDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    FloodgateIntegration floodgateIntegration
  ) {
    super(config, plugin, DonorSymbolColorDisplay.class);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected DonorSymbolColorDisplay instantiateDisplay(Player player, ColorSelectionData displayData) {
    return new DonorSymbolColorDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, DonorSymbolColorDisplay display, ClickType clickType, int slot) {
    var color = display.getColorBySlotIndex(slot);

    if (color != null) {
      if (clickType == ClickType.LEFT) {
        if (display.displayData.profile().color == color) {
          config.rootSection.donorSymbol.colorDisplay.colorAlreadySelected.sendMessage(player, color.makeEnvironment());
          return;
        }

        display.displayData.profile().color = color;
        config.rootSection.donorSymbol.colorDisplay.colorSelected.sendMessage(player, color.makeEnvironment());
        display.updateItems();
        return;
      }

      return;
    }

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.donorSymbol.symbolDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.donorSymbol.symbolDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (config.rootSection.donorSymbol.symbolDisplay.items.backButton.getDisplaySlots().contains(slot)) {
        display.displayData.backButton().run();
        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.donorSymbol.symbolDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.donorSymbol.symbolDisplay.items.nextPage.getDisplaySlots().contains(slot))
        display.lastPage();
    }
  }
}

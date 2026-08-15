package me.blvckbytes.bbtweaks.donor_symbol.symbol_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class DonorSymbolSymbolDisplayHandler extends DisplayHandler<DonorSymbolSymbolDisplay, SymbolSelectionData> {

  private final FloodgateIntegration floodgateIntegration;

  public DonorSymbolSymbolDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    FloodgateIntegration floodgateIntegration
  ) {
    super(config, plugin, DonorSymbolSymbolDisplay.class);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected DonorSymbolSymbolDisplay instantiateDisplay(Player player, SymbolSelectionData displayData) {
    return new DonorSymbolSymbolDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, DonorSymbolSymbolDisplay display, ClickType clickType, int slot) {
    var symbol = display.getSymbolBySlotIndex(slot);

    if (symbol != null) {
      if (clickType == ClickType.LEFT) {
        if (display.displayData.profile().symbol == symbol) {
          config.rootSection.donorSymbol.symbolDisplay.symbolAlreadySelected.sendMessage(player, symbol.makeEnvironment());
          return;
        }

        display.displayData.profile().symbol = symbol;
        config.rootSection.donorSymbol.symbolDisplay.symbolSelected.sendMessage(player, symbol.makeEnvironment());
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

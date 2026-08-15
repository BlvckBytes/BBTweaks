package me.blvckbytes.bbtweaks.donor_symbol.main_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.color_display.ColorSelectionData;
import me.blvckbytes.bbtweaks.donor_symbol.color_display.DonorSymbolColorDisplayHandler;
import me.blvckbytes.bbtweaks.donor_symbol.profile.DonorSymbolProfile;
import me.blvckbytes.bbtweaks.donor_symbol.symbol_display.DonorSymbolSymbolDisplayHandler;
import me.blvckbytes.bbtweaks.donor_symbol.symbol_display.SymbolSelectionData;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class DonorSymbolDisplayHandler extends DisplayHandler<DonorSymbolDisplay, DonorSymbolProfile> {

  private final DonorSymbolSymbolDisplayHandler symbolDisplayHandler;
  private final DonorSymbolColorDisplayHandler colorDisplayHandler;
  private final FloodgateIntegration floodgateIntegration;

  public DonorSymbolDisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    DonorSymbolSymbolDisplayHandler symbolDisplayHandler,
    DonorSymbolColorDisplayHandler colorDisplayHandler,
    FloodgateIntegration floodgateIntegration
  ) {
    super(config, plugin, DonorSymbolDisplay.class);

    this.symbolDisplayHandler = symbolDisplayHandler;
    this.colorDisplayHandler = colorDisplayHandler;
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected DonorSymbolDisplay instantiateDisplay(Player player, DonorSymbolProfile displayData) {
    return new DonorSymbolDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, DonorSymbolDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.donorSymbol.mainDisplay.items.symbol.getDisplaySlots().contains(slot)) {
      var data = new SymbolSelectionData(display.displayData, display::showNextTick);
      Bukkit.getScheduler().runTaskLater(plugin, () -> symbolDisplayHandler.show(player, data), 1L);
      return;
    }

    if (config.rootSection.donorSymbol.mainDisplay.items.color.getDisplaySlots().contains(slot)) {
      var data = new ColorSelectionData(display.displayData, display::showNextTick);
      Bukkit.getScheduler().runTaskLater(plugin, () -> colorDisplayHandler.show(player, data), 1L);
      return;
    }

    if (config.rootSection.donorSymbol.mainDisplay.items.enabled.getDisplaySlots().contains(slot)) {
      display.displayData.enabled ^= true;
      display.updateItems();
      // TODO: Toggle-message?
    }
  }
}

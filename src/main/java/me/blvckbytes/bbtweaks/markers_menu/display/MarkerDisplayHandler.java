package me.blvckbytes.bbtweaks.markers_menu.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.markers_menu.CategorySection;
import me.blvckbytes.bbtweaks.markers_menu.MarkerSection;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MarkerDisplayHandler extends DisplayHandler<MarkerDisplay, MarkerDisplayData> {

  private final Logger logger;
  private final FloodgateIntegration floodgateIntegration;

  public MarkerDisplayHandler(
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(config, plugin);

    this.logger = plugin.getLogger();
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public MarkerDisplay instantiateDisplay(Player player, MarkerDisplayData displayData) {
    return new MarkerDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, MarkerDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    var displayItem = display.getDisplayItemForSlot(slot);

    if (displayItem != null) {
      if (displayItem instanceof MarkerSection markerSection) {
        teleportToMarker(player, markerSection);
        return;
      }

      if (displayItem instanceof CategorySection categorySection) {
        show(player, new MarkerDisplayData(categorySection, display));
        return;
      }

      logger.log(Level.WARNING, "Don't know how to handle a MarkerDisplayItem of type " + displayItem.getClass());
      return;
    }

    if (config.rootSection.markersMenu.display.items.backToCategoriesButton.getDisplaySlots().contains(slot)) {
      var previousDisplay = display.displayData.previousDisplay();

      if (previousDisplay != null)
        reopen(previousDisplay);

      return;
    }

    if (config.rootSection.markersMenu.display.items.previousPage.getDisplaySlots().contains(slot)) {
      display.previousPage();
      return;
    }

    if (config.rootSection.markersMenu.display.items.nextPage.getDisplaySlots().contains(slot))
      display.nextPage();
  }

  private void teleportToMarker(Player player, MarkerSection marker) {
    if (marker._command != null) {
      player.closeInventory();
      player.performCommand(marker._command);
      return;
    }

    if (marker.location != null) {
      if (marker.location._location != null) {
        player.closeInventory();
        player.teleport(marker.location._location);

        config.rootSection.markersMenu.messages.teleportedToLocation.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("name", marker.getDisplayNameOrName())
        );

        return;
      }
    }

    config.rootSection.markersMenu.messages.noLocationSet.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("name", marker.getDisplayNameOrName())
    );
  }
}

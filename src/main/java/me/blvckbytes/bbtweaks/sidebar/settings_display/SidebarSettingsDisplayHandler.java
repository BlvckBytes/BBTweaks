package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.color_display.ColorDisplayData;
import me.blvckbytes.bbtweaks.sidebar.color_display.SidebarColorDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SidebarSortingDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SortingDisplayData;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class SidebarSettingsDisplayHandler extends DisplayHandler<SidebarSettingsDisplay, SidebarPreferences> {

  private final SidebarColorDisplayHandler sidebarColorDisplayHandler;
  private final SidebarSortingDisplayHandler sidebarSortingDisplayHandler;
  private final FloodgateIntegration floodgateIntegration;

  public SidebarSettingsDisplayHandler(
    SidebarColorDisplayHandler sidebarColorDisplayHandler,
    SidebarSortingDisplayHandler sidebarSortingDisplayHandler,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, SidebarSettingsDisplay.class);

    this.sidebarColorDisplayHandler = sidebarColorDisplayHandler;
    this.sidebarSortingDisplayHandler = sidebarSortingDisplayHandler;
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  protected SidebarSettingsDisplay instantiateDisplay(Player player, SidebarPreferences displayData) {
    return new SidebarSettingsDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarSettingsDisplay display, ClickType clickType, int slot) {
    var statistic = display.getStatisticBySlotIndex(slot);

    if (statistic != null) {
      if (clickType == ClickType.LEFT) {
        display.displayData.enableModeByStatistic.computeIfPresent(
          statistic._sidebarStatistic,
          (sidebarStatistic, currentMode) -> currentMode.next(sidebarStatistic)
        );

        display.renderItems();
        return;
      }

      if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
        if (statistic._sidebarStatistic.isSpacer)
          return;

        var displayData = new ColorDisplayData(
          display.displayData, statistic,
          display::showNextTick
        );

        sidebarColorDisplayHandler.show(player, displayData);
      }
      return;
    }

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.sidebar.settingsDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.showTitle.getDisplaySlots().contains(slot)) {
        display.displayData.showTitle ^= true;
        display.renderItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.showIcons.getDisplaySlots().contains(slot)) {
        display.displayData.showIcons ^= true;
        display.renderItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.delimitersMode.getDisplaySlots().contains(slot)) {
        display.displayData.delimitersMode = display.displayData.delimitersMode.next();
        display.renderItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.nextSneakMode.getDisplaySlots().contains(slot)) {
        display.displayData.sneakMode = display.displayData.sneakMode.next();
        display.renderItems();
        return;
      }

      // TODO: This most definitely needs a confirmation-UI
      if (config.rootSection.sidebar.settingsDisplay.items.resetToDefaults.getDisplaySlots().contains(slot)) {
        if (!display.displayData.divergesFromDefaults()) {
          config.rootSection.sidebar.noChangesMadeToReset.sendMessage(player);
          return;
        }

        display.displayData.resetToDefaults();
        display.renderItems();

        config.rootSection.sidebar.settingsHaveBeenReset.sendMessage(player);
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.allColors.getDisplaySlots().contains(slot)) {
        var displayData = new ColorDisplayData(
          display.displayData, null,
          display::showNextTick
        );

        sidebarColorDisplayHandler.show(player, displayData);
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.openSorting.getDisplaySlots().contains(slot)) {
        sidebarSortingDisplayHandler.show(player, new SortingDisplayData(
          display.displayData,
          display::showNextTick
        ));

        return;
      }

      return;
    }

    if (clickType == ClickType.RIGHT) {
      if (config.rootSection.sidebar.settingsDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.firstPage();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.nextPage.getDisplaySlots().contains(slot))
        display.lastPage();
    }
  }
}

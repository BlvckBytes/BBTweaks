package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.color_display.ColorDisplayData;
import me.blvckbytes.bbtweaks.sidebar.color_display.SidebarColorDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesSlots;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SidebarSortingDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SortingDisplayData;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class SidebarSettingsDisplayHandler extends DisplayHandler<SidebarSettingsDisplay, SidebarPreferencesSlots> {

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
  protected SidebarSettingsDisplay instantiateDisplay(Player player, SidebarPreferencesSlots displayData) {
    return new SidebarSettingsDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarSettingsDisplay display, ClickType clickType, int slot) {
    var preferences = display.displayData.getSelectedPreferences();
    var statistic = display.getStatisticBySlotIndex(slot);

    if (statistic != null) {
      if (clickType == ClickType.LEFT) {
        preferences.enableModeByStatistic.computeIfPresent(
          statistic._sidebarStatistic,
          (sidebarStatistic, currentMode) -> currentMode.next(sidebarStatistic)
        );

        display.updateItems();
        return;
      }

      if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
        if (statistic._sidebarStatistic.isSpacer)
          return;

        var displayData = new ColorDisplayData(
          preferences, statistic,
          display::showNextTick
        );

        sidebarColorDisplayHandler.show(player, displayData);
      }
      return;
    }

    if (clickType == ClickType.LEFT) {
      if (config.rootSection.sidebar.settingsDisplay.items.enabled.getDisplaySlots().contains(slot)) {
        display.displayData.setEnabled(null);
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.previousPage.getDisplaySlots().contains(slot)) {
        display.previousPage();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.nextPage.getDisplaySlots().contains(slot)) {
        display.nextPage();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.showTitle.getDisplaySlots().contains(slot)) {
        preferences.showTitle ^= true;
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.showIcons.getDisplaySlots().contains(slot)) {
        preferences.showIcons ^= true;
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.doScroll.getDisplaySlots().contains(slot)) {
        preferences.doScroll ^= true;
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.delimitersMode.getDisplaySlots().contains(slot)) {
        preferences.delimitersMode = preferences.delimitersMode.next();
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.nextSneakMode.getDisplaySlots().contains(slot)) {
        preferences.sneakMode = preferences.sneakMode.next();
        display.updateItems();
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.allColors.getDisplaySlots().contains(slot)) {
        var displayData = new ColorDisplayData(
          preferences, null,
          display::showNextTick
        );

        sidebarColorDisplayHandler.show(player, displayData);
        return;
      }

      if (config.rootSection.sidebar.settingsDisplay.items.openSorting.getDisplaySlots().contains(slot)) {
        sidebarSortingDisplayHandler.show(player, new SortingDisplayData(
          preferences,
          display.getCurrentPage(),
          display::showNextTick
        ));

        return;
      }

      var slotsSlotIndices = config.rootSection.sidebar.settingsDisplay.items.preferencesSlot.getDisplaySlots();

      if (slotsSlotIndices.contains(slot)) {
        var parametersSlotIndex = (int) slotsSlotIndices.stream().filter(it -> it < slot).count();
        display.displayData.setSelectedSlotIndex(parametersSlotIndex, true);
        display.updateItems();
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

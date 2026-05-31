package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.color_display.SidebarColorDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SidebarSortingDisplayHandler;
import me.blvckbytes.bbtweaks.sidebar.sorting_display.SortingDisplayData;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.Bukkit;
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
    super(config, plugin);

    this.sidebarColorDisplayHandler = sidebarColorDisplayHandler;
    this.sidebarSortingDisplayHandler = sidebarSortingDisplayHandler;
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public SidebarSettingsDisplay instantiateDisplay(Player player, SidebarPreferences displayData) {
    return new SidebarSettingsDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarSettingsDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.sidebar.settingsDisplay.items.enabled.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.toggleEnabled();

      display.renderItems();
      return;
    }

    if (config.rootSection.sidebar.settingsDisplay.items.showTitle.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.showTitle ^= true;

      display.renderItems();
      return;
    }

    if (config.rootSection.sidebar.settingsDisplay.items.nextSneakMode.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.sneakMode = display.displayData.sneakMode.next();

      display.renderItems();
      return;
    }

    if (config.rootSection.sidebar.settingsDisplay.items.valueColor.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      sidebarColorDisplayHandler.show(player, color -> {
        if (color != null)
          display.displayData.valueColor = color;

        Bukkit.getScheduler().runTaskLater(plugin, () -> reopen(display), 1L);
      });

      return;
    }

    if (config.rootSection.sidebar.settingsDisplay.items.openSorting.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      sidebarSortingDisplayHandler.show(player, new SortingDisplayData(
        display.displayData,
        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> reopen(display), 1L)
      ));

      return;
    }

    var statistic = display.getStatisticBySlotIndex(slot);

    if (statistic == null)
      return;

    if (clickType == ClickType.LEFT) {
      if (display.displayData.enabledStatistics.contains(statistic._sidebarStatistic))
        display.displayData.enabledStatistics.remove(statistic._sidebarStatistic);
      else
        display.displayData.enabledStatistics.add(statistic._sidebarStatistic);

      display.renderItems();
      return;
    }

    if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
      sidebarColorDisplayHandler.show(player, color -> {
        if (color != null)
          display.displayData.labelColorByStatistic.put(statistic._sidebarStatistic, color);

        Bukkit.getScheduler().runTaskLater(plugin, () -> reopen(display), 1L);
      });

      display.renderItems();
    }
  }
}

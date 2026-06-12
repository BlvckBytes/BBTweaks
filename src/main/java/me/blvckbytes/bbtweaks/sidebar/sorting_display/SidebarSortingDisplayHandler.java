package me.blvckbytes.bbtweaks.sidebar.sorting_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

import java.util.stream.Stream;

public class SidebarSortingDisplayHandler extends DisplayHandler<SidebarSortingDisplay, SortingDisplayData> {

  private final FloodgateIntegration floodgateIntegration;

  public SidebarSortingDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public SidebarSortingDisplay instantiateDisplay(Player player, SortingDisplayData displayData) {
    return new SidebarSortingDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, SidebarSortingDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.sidebar.sortingDisplay.items.backButton.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      display.displayData.backHandler().run();
      return;
    }

    if (config.rootSection.sidebar.sortingDisplay.items.moveDisabledToEnd.getDisplaySlots().contains(slot)) {
      if (clickType != ClickType.LEFT)
        return;

      var list = display.displayData.preferences().statisticsInOrder;

      var newOrder = Stream.concat(
        list.stream().filter(it -> display.displayData.preferences().enableModeByStatistic.get(it).enabled),
        list.stream().filter(it -> !display.displayData.preferences().enableModeByStatistic.get(it).enabled)
      ).toList();

      if (newOrder.equals(list)) {
        config.rootSection.sidebar.allDeactivatedItemsAlreadyAtEnd.sendMessage(player);
        return;
      }

      list.clear();
      list.addAll(newOrder);

      display.renderItems();
      return;
    }

    var statistic = display.getStatisticBySlotIndex(slot);

    if (statistic == null)
      return;

    if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
      var list = display.displayData.preferences().statisticsInOrder;
      var currentIndex = list.indexOf(statistic);

      if (currentIndex >= list.size() - 1) {
        config.rootSection.sidebar.entryAlreadyAtTheVeryEnd.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("name", config.rootSection.sidebar._statisticsMap.get(statistic).iconData.name.markupNode)
        );

        return;
      }

      list.remove(currentIndex);
      list.add(currentIndex + 1, statistic);

      config.rootSection.sidebar.movedAllDeactivatedItemsToEnd.sendMessage(player);

      display.renderItems();
      return;
    }

    if (clickType == ClickType.LEFT) {
      var list = display.displayData.preferences().statisticsInOrder;
      var currentIndex = list.indexOf(statistic);

      if (currentIndex <= 0) {
        config.rootSection.sidebar.entryAlreadyAtTheVeryBeginning.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("name", config.rootSection.sidebar._statisticsMap.get(statistic).iconData.name.markupNode)
        );

        return;
      }

      list.remove(currentIndex);
      list.add(currentIndex - 1, statistic);

      display.renderItems();
    }
  }
}

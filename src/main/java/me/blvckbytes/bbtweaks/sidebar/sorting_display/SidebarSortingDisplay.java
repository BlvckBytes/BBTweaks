package me.blvckbytes.bbtweaks.sidebar.sorting_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SidebarSortingDisplay extends Display<SortingDisplayData> {

  private final Int2ObjectMap<SidebarStatistic> statisticBySlotIndex;

  public final boolean isFloodgate;

  public SidebarSortingDisplay(
    Player player,
    SortingDisplayData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    this.statisticBySlotIndex = new Int2ObjectOpenHashMap<>();

    show();
  }

  @Override
  protected void renderItems() {
    inventory.clear();

    var environment = makeEnvironment();

    config.rootSection.sidebar.sortingDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.sidebar.sortingDisplay.items.backButton.renderInto(inventory, environment);
    config.rootSection.sidebar.sortingDisplay.items.moveDisabledToEnd.renderInto(inventory, environment);

    for (var index = 0; index < inventory.getSize(); ++index) {
      if (index >= displayData.preferences().statisticsInOrder.size())
        break;

      var statistic = displayData.preferences().statisticsInOrder.get(index);

      statisticBySlotIndex.put(index, statistic);

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      var enableMode = displayData.preferences().enableModeByStatistic.get(statistic);

      environment
        .withVariable("name", statisticSection.iconData.name.markupNode)
        .withVariable("description", statisticSection.iconData.description.markupNode)
        .withVariable("icon_type", statisticSection.iconData._iconType)
        .withVariable("enabled", enableMode.enabled)
        .withVariable("show_label", enableMode.showLabel);

      inventory.setItem(index, config.rootSection.sidebar.sortingDisplay.items.statisticIcon.build(environment));
    }
  }

  public @Nullable SidebarStatistic getStatisticBySlotIndex(int slotIndex) {
    return statisticBySlotIndex.get(slotIndex);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.sidebar.sortingDisplay.createInventory(makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate);
  }
}

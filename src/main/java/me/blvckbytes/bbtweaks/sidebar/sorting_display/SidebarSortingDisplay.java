package me.blvckbytes.bbtweaks.sidebar.sorting_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.SidebarStatistic;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SidebarSortingDisplay extends Display<SortingDisplayData> {

  public final boolean isFloodgate;

  private final SidebarStatistic[] slotMap;
  private int numberOfPages;

  private int currentPage;

  public SidebarSortingDisplay(
    Player player,
    SortingDisplayData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    this.slotMap = new SidebarStatistic[9 * 6];

    this.currentPage = displayData.initialPage();
  }

  public void nextPage() {
    if (currentPage >= numberOfPages)
      return;

    ++currentPage;
    showNextTick();
  }

  public void previousPage() {
    if (currentPage <= 1)
      return;

    --currentPage;
    showNextTick();
  }

  public void firstPage() {
    if (currentPage <= 1)
      return;

    currentPage = 1;
    showNextTick();
  }

  public void lastPage() {
    if (currentPage >= numberOfPages)
      return;

    currentPage = numberOfPages;
    showNextTick();
  }

  private void updateNumberOfPages() {
    var numberOfDisplaySlots = config.rootSection.sidebar.settingsDisplay.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(displayData.preferences().statisticsInOrder.size() / (double) numberOfDisplaySlots));

    if (currentPage > numberOfPages)
      currentPage = numberOfPages;

    if (currentPage < 1)
      currentPage = 1;
  }

  @Override
  public void show() {
    updateNumberOfPages();
    super.show();
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.sidebar.sortingDisplay.items.previousPage.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.sortingDisplay.items.backButton.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.sortingDisplay.items.moveDisabledToEnd.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.sortingDisplay.items.nextPage.renderInto(itemConsumer, environment);

    var displaySlots = config.rootSection.sidebar.settingsDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = displayData.preferences().statisticsInOrder.size();

    for (var slot : displaySlots) {
      var currentItemIndex = itemsIndex++;

      if (currentItemIndex >= numberOfItems) {
        slotMap[slot] = null;
        itemConsumer.handle(slot, null);
        continue;
      }

      var statistic = displayData.preferences().statisticsInOrder.get(currentItemIndex);

      slotMap[slot] = statistic;

      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      var enableMode = displayData.preferences().enableModeByStatistic.get(statistic);

      environment
        .withVariable("name", statisticSection.iconData.name.markupNode)
        .withVariable("description", statisticSection.iconData.description.markupNode)
        .withVariable("icon_type", statisticSection.iconData._iconType)
        .withVariable("enabled", enableMode.enabled)
        .withVariable("show_label", enableMode.showLabel);

      itemConsumer.handle(slot, config.rootSection.sidebar.sortingDisplay.items.statisticIcon.build(environment));
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.sidebar.sortingDisplay.items.filler;
  }

  public @Nullable SidebarStatistic getStatisticBySlotIndex(int slotIndex) {
    return slotMap[slotIndex];
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.sidebar.sortingDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("current_page", currentPage)
      .withVariable("number_pages", numberOfPages)
      .withVariable("is_floodgate", isFloodgate);
  }
}

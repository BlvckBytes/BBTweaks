package me.blvckbytes.bbtweaks.sidebar.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.config.StatisticSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SidebarSettingsDisplay extends Display<SidebarPreferences> {

  public final boolean isFloodgate;

  private final StatisticSection[] slotMap;
  private int numberOfPages;

  private int currentPage = 1;

  public SidebarSettingsDisplay(
    Player player,
    SidebarPreferences displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    this.slotMap = new StatisticSection[9 * 6];
  }

  public int getCurrentPage() {
    return currentPage;
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

  @Override
  public void show() {
    updateNumberOfPages();
    super.show();
  }

  private void updateNumberOfPages() {
    var numberOfDisplaySlots = config.rootSection.sidebar.settingsDisplay.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(displayData.statisticsInOrder.size() / (double) numberOfDisplaySlots));

    if (currentPage > numberOfPages)
      currentPage = numberOfPages;
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.sidebar.settingsDisplay.items.previousPage.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.showTitle.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.showIcons.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.doScroll.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.delimitersMode.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.allColors.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.nextSneakMode.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.openSorting.renderInto(itemConsumer, environment);
    config.rootSection.sidebar.settingsDisplay.items.nextPage.renderInto(itemConsumer, environment);

    var displaySlots = config.rootSection.sidebar.settingsDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = displayData.statisticsInOrder.size();

    for (var slot : displaySlots) {
      var currentItemIndex = itemsIndex++;

      if (currentItemIndex >= numberOfItems) {
        slotMap[slot] = null;
        itemConsumer.handle(slot, null);
        continue;
      }

      var statistic = displayData.statisticsInOrder.get(currentItemIndex);
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);

      slotMap[slot] = statisticSection;

      var enableMode = displayData.enableModeByStatistic.get(statistic);

      environment
        .withVariable("name", statisticSection.iconData.name.markupNode)
        .withVariable("description", statisticSection.iconData.description.markupNode)
        .withVariable("icon_type", statisticSection.iconData._iconType)
        .withVariable("label_style", displayData.labelStyleByStatistic.get(statistic))
        .withVariable("value_style", displayData.valueStyleByStatistic.get(statistic))
        .withVariable("enabled", enableMode.enabled)
        .withVariable("show_label", enableMode.showLabel)
        .withVariable("is_spacer", statistic.isSpacer);

      itemConsumer.handle(slot, config.rootSection.sidebar.settingsDisplay.items.statisticIcon.build(environment));
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.sidebar.settingsDisplay.items.filler;
  }

  public @Nullable StatisticSection getStatisticBySlotIndex(int slotIndex) {
    return slotMap[slotIndex];
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.sidebar.settingsDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("current_page", currentPage)
      .withVariable("number_pages", numberOfPages)
      .withVariable("sidebar_enabled", displayData.enabled)
      .withVariable("show_title", displayData.showTitle)
      .withVariable("show_icons", displayData.showIcons)
      .withVariable("do_scroll", displayData.doScroll)
      .withVariable("delimiters_mode", displayData.delimitersMode.name())
      .withVariable("sneak_mode", displayData.sneakMode.name());
  }
}

package me.blvckbytes.bbtweaks.markers_menu.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarkerDisplay extends Display<MarkerDisplayData> {

  public final boolean isFloodgate;

  private final MarkerDisplayItem[] slotMap;

  private final List<? extends MarkerDisplayItem> displayItems;
  private final int numberOfPages;

  private int currentPage = 1;

  protected MarkerDisplay(
    Player player,
    MarkerDisplayData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);
    this.slotMap = new MarkerDisplayItem[9 * 6];

    displayItems = displayData.selectedCategory() == null
      ? new ArrayList<>(config.rootSection.markersMenu.categories.values())
      : displayData.selectedCategory()._members;

    var numberOfDisplaySlots = config.rootSection.markersMenu.display.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(displayItems.size() / (double) numberOfDisplaySlots));

    show();
  }

  public @Nullable MarkerDisplayItem getDisplayItemForSlot(int slot) {
    if (slot < 0 || slot >= slotMap.length)
      return null;

    return slotMap[slot];
  }

  public void nextPage() {
    if (currentPage == numberOfPages)
      return;

    ++currentPage;
    show();
  }

  public void previousPage() {
    if (currentPage == 1)
      return;

    --currentPage;
    show();
  }

  @Override
  protected void renderItems() {
    var environment = getEnvironment();

    var displaySlots = new ArrayList<>(config.rootSection.markersMenu.display.getPaginationSlots());
    var itemsIndex = (currentPage - 1) * displaySlots.size();

    for (Integer slot : displaySlots) {
      var currentSlot = itemsIndex++;

      if (currentSlot >= displayItems.size()) {
        slotMap[slot] = null;
        inventory.setItem(slot, null);
        continue;
      }

      var renderable = displayItems.get(currentSlot);
      inventory.setItem(slot, renderable.makeRepresentative(environment, config));
      slotMap[slot] = renderable;
    }

    config.rootSection.markersMenu.display.items.filler.renderInto(inventory, environment);
    config.rootSection.markersMenu.display.items.previousPage.renderInto(inventory, environment);
    config.rootSection.markersMenu.display.items.nextPage.renderInto(inventory, environment);

    if (displayData.previousDisplay() != null)
      config.rootSection.markersMenu.display.items.backToCategoriesButton.renderInto(inventory, environment);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.markersMenu.display.createInventory(getEnvironment());
  }

  @Override
  public void onConfigReload() {}

  private InterpretationEnvironment getEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("current_page", this.currentPage)
      .withVariable("number_pages", this.numberOfPages)
      .withVariable("is_floodgate", this.isFloodgate)
      .withVariable("selected_category", displayData.selectedCategory() == null ? null : displayData.selectedCategory().getDisplayNameOrName());
  }
}

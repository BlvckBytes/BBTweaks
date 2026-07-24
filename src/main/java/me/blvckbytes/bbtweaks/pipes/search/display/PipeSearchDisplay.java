package me.blvckbytes.bbtweaks.pipes.search.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class PipeSearchDisplay extends Display<SearchDisplayData> {

  private final SearchDisplayEntry[] slotMap;
  private int numberOfPages;

  private int currentPage = 1;

  private CollectionAction collectionAction = CollectionAction.first();
  private StackAction stackAction = StackAction.first();

  public final boolean isFloodgate;

  public PipeSearchDisplay(
    Player player,
    FloodgateIntegration floodgateIntegration,
    SearchDisplayData displayData,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.slotMap = new SearchDisplayEntry[9 * 6];

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);
  }

  @Override
  public void onConfigReload() {
    show();
  }

  public @Nullable SearchDisplayEntry getEntryCorrespondingToSlot(int slot) {
    return slotMap[slot];
  }

  public void removeEntry(SearchDisplayEntry entry) {
    displayData.entries().remove(entry);

    var priorNumberOfPages = numberOfPages;

    updateNumberOfPages();

    // Avoid reopening the inventory if the title did not change
    if (priorNumberOfPages == numberOfPages) {
      renderItems();
      return;
    }

    showNextTick();
  }

  private void clearSlotMap() {
    for (var i = 0; i < slotMap.length; ++i)
      this.slotMap[i] = null;
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

  public void nextCollectionAction() {
    this.collectionAction = this.collectionAction.nextAction();
    renderItems();
  }

  public void nextStackAction() {
    this.stackAction = this.stackAction.nextAction();
    renderItems();
  }

  public CollectionAction getCollectionAction() {
    return this.collectionAction;
  }

  public StackAction getStackAction() {
    return this.stackAction;
  }

  private void updateNumberOfPages() {
    var numberOfDisplaySlots = config.rootSection.pipes.search.display.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(displayData.entries().size() / (double) numberOfDisplaySlots));
  }

  @Override
  public void show() {
    updateNumberOfPages();

    // Since we're removing items when handing them out, automatically page back if it was the last on the current page
    if (currentPage > numberOfPages)
      currentPage = numberOfPages;

    clearSlotMap();

    super.show();
  }

  @Override
  public void renderItems() {
    var displaySlots = config.rootSection.pipes.search.display.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = displayData.entries().size();

    var environment = makeEnvironment();

    for (var paginationSlotIndex = 0; paginationSlotIndex < displaySlots.size(); ++paginationSlotIndex) {
      var slot = displaySlots.get(paginationSlotIndex);
      var currentSlot = itemsIndex++;

      if (currentSlot >= numberOfItems) {
        slotMap[slot] = null;
        inventory.setItem(slot, null);
        continue;
      }

      var entry = displayData.entries().get(currentSlot);

      ItemStack representativeItem;

      try {
        representativeItem = entry.makeRepresentative(environment, config);

        // Just re-using the handler below.
        if (representativeItem == null)
          throw new IllegalStateException();
      }
      // java.lang.IllegalStateException: Could not get meta of item
      // The above occurs if the item has been moved; simply remove such items from the UI as well.
      catch (IllegalStateException ignored) {
        // Try again with the next item at the same slot
        displayData.entries().remove(entry);
        --itemsIndex;
        --paginationSlotIndex;
        --numberOfItems;
        continue;
      }

      inventory.setItem(slot, representativeItem);

      slotMap[slot] = entry;
    }

    config.rootSection.pipes.search.display.items.filler.renderInto(inventory, environment);
    config.rootSection.pipes.search.display.items.previousPage.renderInto(inventory, environment);
    config.rootSection.pipes.search.display.items.nextPage.renderInto(inventory, environment);

    if (displayData.backToDisplay() != null)
      config.rootSection.pipes.search.display.items.backToCollectionsButton.renderInto(inventory, environment);

    if (displayData.predicate() != null)
      config.rootSection.pipes.search.display.items.searchDetails.renderInto(inventory, environment);
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.pipes.search.display, makeEnvironment());
  }

  private InterpretationEnvironment makeEnvironment() {
    var environment = config.rootSection.pipes.search.display.inventoryEnvironment.copy()
      .withVariable("predicate", displayData.predicate() == null ? null : displayData.predicate().getTokenPredicateString())
      .withVariable("current_page", currentPage)
      .withVariable("number_pages", numberOfPages)
      .withVariable("is_floodgate", isFloodgate);

    if (isFloodgate) {
      environment.withVariable(
        "collection_actions",
        CollectionAction.values.stream().map(action -> new EnumEntry(action, action == collectionAction)).toList()
      );

      environment.withVariable(
        "stack_actions",
        StackAction.values.stream().map(action -> new EnumEntry(action, action == stackAction)).toList()
      );
    }

    return environment;
  }
}

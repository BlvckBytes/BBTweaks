package me.blvckbytes.bbtweaks.donor_symbol.symbol_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.SymbolSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class DonorSymbolSymbolDisplay extends Display<SymbolSelectionData> {

  private final Int2ObjectMap<SymbolSection> symbolBySlotIndex;

  private int numberOfPages;

  private int currentPage = 1;

  public DonorSymbolSymbolDisplay(
    Player player,
    SymbolSelectionData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);

    this.symbolBySlotIndex = new Int2ObjectOpenHashMap<>();
  }

  public @Nullable SymbolSection getSymbolBySlotIndex(int slot) {
    return symbolBySlotIndex.get(slot);
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
    var numberOfDisplaySlots = config.rootSection.donorSymbol.symbolDisplay.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(config.rootSection.donorSymbol._symbolsInOrder.size() / (double) numberOfDisplaySlots));

    if (currentPage > numberOfPages)
      currentPage = numberOfPages;
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.donorSymbol.symbolDisplay.items.backButton.renderInto(itemConsumer, environment);

    var displaySlots = config.rootSection.donorSymbol.symbolDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = config.rootSection.donorSymbol._symbolsInOrder.size();

    symbolBySlotIndex.clear();

    for (var slot : displaySlots) {
      var currentItemIndex = itemsIndex++;

      if (currentItemIndex >= numberOfItems) {
        itemConsumer.handle(slot, null);
        continue;
      }

      var symbol = config.rootSection.donorSymbol._symbolsInOrder.get(currentItemIndex);

      symbolBySlotIndex.put((int) slot, symbol);

      itemConsumer.handle(
        slot,
        config.rootSection.donorSymbol.symbolDisplay.items.symbol.build(
          symbol.makeEnvironment()
            .inheritFrom(environment, false)
            .inheritFrom(displayData.profile().color.makeEnvironment(), false)
            .withVariable("selected", symbol == displayData.profile().symbol)
        )
      );
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.donorSymbol.symbolDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.donorSymbol.symbolDisplay, makeEnvironment());
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

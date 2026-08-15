package me.blvckbytes.bbtweaks.donor_symbol.color_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.donor_symbol.ColorSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class DonorSymbolColorDisplay extends Display<ColorSelectionData> {

  private final Int2ObjectMap<ColorSection> colorBySlotIndex;

  private int numberOfPages;

  private int currentPage = 1;

  public DonorSymbolColorDisplay(
    Player player,
    ColorSelectionData displayData,
    ConfigKeeper<MainSection> config,
    FloodgateIntegration floodgateIntegration,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);

    this.colorBySlotIndex = new Int2ObjectOpenHashMap<>();
  }

  public @Nullable ColorSection getColorBySlotIndex(int slot) {
    return colorBySlotIndex.get(slot);
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
    var numberOfDisplaySlots = config.rootSection.donorSymbol.colorDisplay.getPaginationSlots().size();
    this.numberOfPages = Math.max(1, (int) Math.ceil(config.rootSection.donorSymbol._colorsInOrder.size() / (double) numberOfDisplaySlots));

    if (currentPage > numberOfPages)
      currentPage = numberOfPages;
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.donorSymbol.colorDisplay.items.backButton.renderInto(itemConsumer, environment);

    var displaySlots = config.rootSection.donorSymbol.colorDisplay.getPaginationSlots();
    var itemsIndex = (currentPage - 1) * displaySlots.size();
    var numberOfItems = config.rootSection.donorSymbol._colorsInOrder.size();

    colorBySlotIndex.clear();

    for (var slot : displaySlots) {
      var currentItemIndex = itemsIndex++;

      if (currentItemIndex >= numberOfItems) {
        itemConsumer.handle(slot, null);
        continue;
      }

      var color = config.rootSection.donorSymbol._colorsInOrder.get(currentItemIndex);

      colorBySlotIndex.put((int) slot, color);

      itemConsumer.handle(
        slot,
        config.rootSection.donorSymbol.colorDisplay.items.color.build(
          color.makeEnvironment()
            .inheritFrom(environment, false)
            .inheritFrom(displayData.profile().symbol.makeEnvironment(), false)
            .withVariable("selected", color == displayData.profile().color)
        )
      );
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.donorSymbol.colorDisplay.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.donorSymbol.colorDisplay, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("is_editing_other", displayData.profile().player != player)
      .withVariable("profile_name", displayData.profile().player.getName())
      .withVariable("is_floodgate", isFloodgate);
  }
}

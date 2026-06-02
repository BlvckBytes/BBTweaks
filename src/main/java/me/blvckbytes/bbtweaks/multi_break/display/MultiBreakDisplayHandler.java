package me.blvckbytes.bbtweaks.multi_break.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class MultiBreakDisplayHandler extends DisplayHandler<MultiBreakDisplay, MultiBreakDisplayData> {

  private final FloodgateIntegration floodgateIntegration;
  private final PredicateHelper predicateHelper;

  public MultiBreakDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin);

    this.floodgateIntegration = floodgateIntegration;
    this.predicateHelper = predicateHelper;
  }

  @Override
  public MultiBreakDisplay instantiateDisplay(Player player, MultiBreakDisplayData displayData) {
    return new MultiBreakDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, MultiBreakDisplay display, ClickType clickType, int slot) {
    if (handleSlotClickAndGetIfRender(player, display, clickType, slot))
      display.renderItems();
  }

  private boolean handleSlotClickAndGetIfRender(Player player, MultiBreakDisplay display, ClickType clickType, int slot) {
    var parametersSlots = display.displayData.parametersSlots();
    var selectedParameters = parametersSlots.getSelectedParameters();

    if (config.rootSection.multiBreak.display.items.extentLeft.getDisplaySlots().contains(slot))
      return handleExtentManipulation(display, BreakExtent.LEFT, clickType);

    if (config.rootSection.multiBreak.display.items.extentRight.getDisplaySlots().contains(slot))
      return handleExtentManipulation(display, BreakExtent.RIGHT, clickType);

    if (config.rootSection.multiBreak.display.items.extentUp.getDisplaySlots().contains(slot))
      return handleExtentManipulation(display, BreakExtent.UP, clickType);

    if (config.rootSection.multiBreak.display.items.extentDown.getDisplaySlots().contains(slot))
      return handleExtentManipulation(display, BreakExtent.DOWN, clickType);

    if (config.rootSection.multiBreak.display.items.extentDepth.getDisplaySlots().contains(slot))
      return handleExtentManipulation(display, BreakExtent.DEPTH, clickType);

    var isRightClick = display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT;

    if (config.rootSection.multiBreak.display.items.currentFilter.getDisplaySlots().contains(slot)) {
      if (isRightClick) {
        if (selectedParameters.tellIfLocked())
          return false;

        selectedParameters.removeFilter(display.displayData.commandLabel(), predicateHelper.getSelectedLanguage(player));
        return true;
      }

      if (clickType == ClickType.LEFT) {
        if (selectedParameters.tellIfLocked())
          return false;

        selectedParameters.setFilterEnabled(null);
        return true;
      }

      return false;
    }

    if (config.rootSection.multiBreak.display.items.sneakMode.getDisplaySlots().contains(slot)) {
      if (clickType == ClickType.LEFT) {
        if (selectedParameters.tellIfLocked())
          return false;

        selectedParameters.sneakMode = selectedParameters.sneakMode.next();
        return true;
      }

      return false;
    }

    if (config.rootSection.multiBreak.display.items.toggleEnabled.getDisplaySlots().contains(slot)) {
      if (clickType == ClickType.LEFT) {
        parametersSlots.setEnabled(null);
        return true;
      }

      return false;
    }

    var slotsSlotIndices = config.rootSection.multiBreak.display.items.parametersSlot.getDisplaySlots();

    if (slotsSlotIndices.contains(slot)) {
      var parametersSlotIndex = (int) slotsSlotIndices.stream().filter(it -> it < slot).count();

      if (clickType == ClickType.LEFT) {
        if (parametersSlots.getSelectedSlotIndex() == parametersSlotIndex) {
          config.rootSection.multiBreak.slotAlreadySelected.sendMessage(
            player,
            parametersSlots.getSelectedParameters().makeEnvironment()
          );

          return false;
        }

        parametersSlots.setSelectedSlotIndex(parametersSlotIndex);

        config.rootSection.multiBreak.slotSelected.sendMessage(
          player,
          parametersSlots.getSelectedParameters().makeEnvironment()
        );

        return true;
      }

      if (isRightClick) {
        parametersSlots.parametersBySlotIndex.get(parametersSlotIndex).toggleLocked();
        return true;
      }

      return false;
    }

    return false;
  }

  private boolean handleExtentManipulation(MultiBreakDisplay display, BreakExtent extent, ClickType clickType) {
    var selectedParameters = display.displayData.parametersSlots().getSelectedParameters();

    if (clickType == ClickType.LEFT) {
      if (selectedParameters.tellIfLocked())
        return false;

      selectedParameters.decreaseExtent(extent);
      return true;
    }

    if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
      if (selectedParameters.tellIfLocked())
        return false;

      selectedParameters.increaseExtent(extent);
      return true;
    }

    return false;
  }
}

package me.blvckbytes.bbtweaks.multi_break.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.command.CommandAction;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
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

    if (config.rootSection.multiBreak.display.items.currentFilter.getDisplaySlots().contains(slot)) {
      if (clickType == ClickType.LEFT) {
        var currentFilter = display.displayData.breakParameters().filter;

        if (currentFilter == null) {
          config.rootSection.multiBreak.noFilterSet.sendMessage(player);
          return false;
        }

        config.rootSection.multiBreak.filterRemoved.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("filter_predicate", currentFilter.getTokenPredicateString())
            .withVariable("set_command", makeFilterSetCommand(display.displayData.commandLabel(), predicateHelper.getSelectedLanguage(player), currentFilter))
        );

        display.displayData.breakParameters().filter = null;
        return true;
      }

      return false;
    }

    if (config.rootSection.multiBreak.display.items.sneakMode.getDisplaySlots().contains(slot)) {
      if (clickType == ClickType.LEFT) {
        display.displayData.breakParameters().sneakMode = display.displayData.breakParameters().sneakMode.next();
        return true;
      }

      return false;
    }

    if (config.rootSection.multiBreak.display.items.toggleEnabled.getDisplaySlots().contains(slot)) {
      if (clickType == ClickType.LEFT) {
        display.displayData.breakParameters().enabled ^= true;
        return true;
      }

      return false;
    }

    return false;
  }

  private boolean handleExtentManipulation(MultiBreakDisplay display, BreakExtent extent, ClickType clickType) {
    if (clickType == ClickType.LEFT) {
      display.displayData.breakParameters().decreaseExtent(extent);
      return true;
    }

    if (display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT) {
      display.displayData.breakParameters().increaseExtent(extent);
      return true;
    }

    return false;
  }

  private String makeFilterSetCommand(String label, TranslationLanguage currentLanguage, PredicateAndLanguage predicateAndLanguage) {
    if (currentLanguage == predicateAndLanguage.language)
      return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER) + " " + predicateAndLanguage.getTokenPredicateString();

    return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER_WITH_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(predicateAndLanguage.language) + " " + predicateAndLanguage.getTokenPredicateString();
  }
}

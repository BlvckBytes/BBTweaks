package me.blvckbytes.bbtweaks.mechanic.magnet.edit_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.EditSession;
import me.blvckbytes.bbtweaks.mechanic.magnet.MagnetParameter;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class EditDisplayHandler extends DisplayHandler<EditDisplay, EditSession> {

  private final PredicateHelper predicateHelper;
  private final FloodgateIntegration floodgateIntegration;

  public EditDisplayHandler(
    PredicateHelper predicateHelper,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config, Plugin plugin
  ) {
    super(config, plugin);

    this.predicateHelper = predicateHelper;
    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public EditDisplay instantiateDisplay(Player player, EditSession displayData) {
    return new EditDisplay(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void handleClick(Player player, EditDisplay display, ClickType clickType, int slot) {
    if (config.rootSection.mechanic.magnet.editDisplay.items.save.getDisplaySlots().contains(slot)) {
      player.closeInventory();
      display.displayData.save();
      return;
    }

    if (config.rootSection.mechanic.magnet.editDisplay.items.cancel.getDisplaySlots().contains(slot)) {
      player.closeInventory();
      display.displayData.cancel();
      return;
    }

    if (config.rootSection.mechanic.magnet.editDisplay.items.toggleClickDetection.getDisplaySlots().contains(slot)) {
      display.displayData.clickDetection ^= true;
      display.renderItems();
      return;
    }

    var isDropOrRight = display.isFloodgate && clickType == ClickType.DROP || !display.isFloodgate && clickType == ClickType.RIGHT;

    if (config.rootSection.mechanic.magnet.editDisplay.items.modifyFilter.getDisplaySlots().contains(slot)) {
      if (display.displayData.filter != null) {
        if (isDropOrRight) {
          config.rootSection.mechanic.magnet.editModeFilterRemoved.sendMessage(player);
          display.displayData.filter = null;
          display.renderItems();
          return;
        }

        if (clickType == ClickType.LEFT) {
          var filterLanguage = display.displayData.filter.language();
          var filterLanguageString = TranslationLanguage.matcher.getNormalizedName(filterLanguage);
          var predicateString = new StringifyState(true).appendPredicate(display.displayData.filter.predicate()).toString();

          String command;

          if (predicateHelper.getSelectedLanguage(player) == filterLanguage)
            command = "/mfilter " + predicateString;
          else
            command = "/mfilterl " + filterLanguageString + " " + predicateString;

          config.rootSection.mechanic.magnet.editModeFilterEditSuggestion.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("filter_predicate", predicateString)
              .withVariable("filter_language", filterLanguageString)
              .withVariable("filter_command", command)
          );

          player.closeInventory();
          return;
        }

        return;
      }

      if (clickType == ClickType.LEFT) {
        config.rootSection.mechanic.magnet.editModeFilterPrompt.sendMessage(player);
        player.closeInventory();
        return;
      }

      return;
    }

    MagnetParameter targetParameter = decideMagnetParameter(display, slot);

    if (targetParameter == null)
      return;

    if (display.displayData.getCurrentParameter() != targetParameter) {
      if (clickType == ClickType.LEFT) {
        display.displayData.setParameter(targetParameter);
        display.renderItems();
      }

      return;
    }

    if (clickType == ClickType.LEFT) {
      display.displayData.decreaseParameter();
      display.renderItems();
      return;
    }

    if (isDropOrRight) {
      display.displayData.increaseParameter();
      display.renderItems();
    }
  }

  private @Nullable MagnetParameter decideMagnetParameter(EditDisplay display, int slot) {
    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentX.getDisplaySlots().contains(slot))
      return display.displayData.parameters.extentX;

    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentY.getDisplaySlots().contains(slot))
      return display.displayData.parameters.extentY;

    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterExtentZ.getDisplaySlots().contains(slot))
      return display.displayData.parameters.extentZ;

    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetX.getDisplaySlots().contains(slot))
      return display.displayData.parameters.offsetX;

    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetY.getDisplaySlots().contains(slot))
      return display.displayData.parameters.offsetY;

    if (config.rootSection.mechanic.magnet.editDisplay.items.selectParameterOffsetZ.getDisplaySlots().contains(slot))
      return display.displayData.parameters.offsetZ;

    return null;
  }
}

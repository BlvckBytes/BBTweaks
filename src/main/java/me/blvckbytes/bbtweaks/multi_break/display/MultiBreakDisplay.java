package me.blvckbytes.bbtweaks.multi_break.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.command.CommandAction;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class MultiBreakDisplay extends Display<MultiBreakDisplayData> {

  public MultiBreakDisplay(
    Player player,
    MultiBreakDisplayData displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, floodgateIntegration, plugin);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = createEnvironment();

    config.rootSection.multiBreak.display.items.extentLeft.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.extentRight.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.extentUp.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.extentDown.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.extentDepth.renderInto(itemConsumer, environment);

    config.rootSection.multiBreak.display.items.currentFilter.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.sneakMode.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.toggleEnabled.renderInto(itemConsumer, environment);

    config.rootSection.multiBreak.display.items.minY.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.maxY.renderInto(itemConsumer, environment);
    config.rootSection.multiBreak.display.items.toggleAutoTool.renderInto(itemConsumer, environment);

    renderSlotSelectionItems(itemConsumer, environment);
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.multiBreak.display.items.filler;
  }

  private void renderSlotSelectionItems(ItemConsumer itemConsumer, InterpretationEnvironment displayEnvironment) {
    var itemSection = config.rootSection.multiBreak.display.items.parametersSlot;
    var slotsInOrder = itemSection.getDisplaySlots().stream().sorted().toList();

    var parametersSlotIndex = 0;
    var parametersSlots = displayData.parametersSlots().parametersBySlotIndex;

    for (var slot : slotsInOrder) {
      if (parametersSlotIndex >= parametersSlots.size())
        break;

      var parametersSlot = parametersSlots.get(parametersSlotIndex++);
      var slotEnvironment = parametersSlot.makeEnvironment().inheritFrom(displayEnvironment, false);

      itemConsumer.handle(slot, itemSection.build(slotEnvironment));
    }
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.multiBreak.display, createEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment createEnvironment() {
    return displayData.parametersSlots().getSelectedParameters().makeEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("filter_set_command", "/" + displayData.commandLabel() + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER))
      .withVariable("auto_tool", displayData.parametersSlots().autoTool)
      .withVariable("min_y", displayData.parametersSlots().minY)
      .withVariable("max_y", displayData.parametersSlots().maxY);
  }
}

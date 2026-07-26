package me.blvckbytes.bbtweaks.inv_filter.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.inv_filter.command.CommandAction;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class InvFilterDisplay extends Display<InvFilterDisplayData> {

  private final boolean isFloodgate;

  public InvFilterDisplay(
    Player player,
    InvFilterDisplayData displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var environment = makeEnvironment();

    config.rootSection.invFilter.display.items.help.renderInto(
      itemConsumer,
      environment
        .withVariable("label", displayData.commandLabel())
        .withVariable("set_command_action", CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER))
        .withVariable("get_command_action", CommandAction.matcher.getNormalizedName(CommandAction.GET_FILTER))
    );

    config.rootSection.invFilter.display.items.enabled.renderInto(itemConsumer, environment);

    var filterItemSection = config.rootSection.invFilter.display.items.filterSlot;

    var filterIndex = 0;

    for (var slot : filterItemSection.getDisplaySlots()) {
      if (filterIndex >= displayData.profile().getSlotCount())
        break;

      var currentIndex = filterIndex++;
      var filterSlot = displayData.profile().getFilter(currentIndex);

      itemConsumer.handle(
        slot,
        filterItemSection.build(
          environment
            .withVariable("enabled", currentIndex == displayData.profile().getSelectedSlotIndex())
            .withVariable("filter", filterSlot == null ? null : filterSlot.getTokenPredicateString())
            .withVariable("slot", currentIndex + 1)
        )
      );
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return config.rootSection.invFilter.display.items.filler;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.invFilter.display, makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("enabled", displayData.profile().enabled)
      .withVariable("is_floodgate", isFloodgate);
  }
}

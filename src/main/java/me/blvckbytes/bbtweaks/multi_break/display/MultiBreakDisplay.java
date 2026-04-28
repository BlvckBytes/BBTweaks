package me.blvckbytes.bbtweaks.multi_break.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakDimension;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.command.CommandAction;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.FloodgateIntegration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class MultiBreakDisplay extends Display<MultiBreakDisplayData> {

  public final boolean isFloodgate;

  public MultiBreakDisplay(
    Player player,
    MultiBreakDisplayData displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    this.isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    show();
  }

  @Override
  protected void renderItems() {
    var environment = createEnvironment();

    config.rootSection.multiBreak.display.items.filler.renderInto(inventory, environment);

    config.rootSection.multiBreak.display.items.extentLeft.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.extentRight.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.extentUp.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.extentDown.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.extentDepth.renderInto(inventory, environment);

    config.rootSection.multiBreak.display.items.currentFilter.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.sneakMode.renderInto(inventory, environment);
    config.rootSection.multiBreak.display.items.toggleEnabled.renderInto(inventory, environment);
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.multiBreak.display.createInventory(createEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment createEnvironment() {
    var breakParameters = displayData.breakParameters();

    return new InterpretationEnvironment()
      .withVariable("is_floodgate", isFloodgate)
      .withVariable("filter_predicate", breakParameters.filter == null ? null : breakParameters.filter.getTokenPredicateString())
      .withVariable("filter_set_command", "/" + displayData.commandLabel() + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER))
      .withVariable("enabled", breakParameters.enabled)
      .withVariable("sneak_mode", breakParameters.sneakMode.name())
      .withVariable("extent_left", breakParameters.getExtent(BreakExtent.LEFT))
      .withVariable("extent_right", breakParameters.getExtent(BreakExtent.RIGHT))
      .withVariable("extent_up", breakParameters.getExtent(BreakExtent.UP))
      .withVariable("extent_down", breakParameters.getExtent(BreakExtent.DOWN))
      .withVariable("extent_depth", breakParameters.getExtent(BreakExtent.DEPTH))
      .withVariable("dimension_limit", breakParameters.getLimits().maxDimension())
      .withVariable("exceeded_width_limit", breakParameters.didExceedLimit(BreakDimension.WIDTH))
      .withVariable("exceeded_height_limit", breakParameters.didExceedLimit(BreakDimension.HEIGHT))
      .withVariable("exceeded_depth_limit", breakParameters.didExceedLimit(BreakDimension.DEPTH));
  }
}

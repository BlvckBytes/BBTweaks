package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ShowcaseDisplay extends Display<ShowcaseDisplayData> {

  public ShowcaseDisplay(
    Player player,
    ShowcaseDisplayData displayData,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);
  }

  @Override
  protected void renderItems() {
    var containerInventory = getContainerInventory();

    if (containerInventory == null) {
      inventory.setItem(13, displayData.frameItem());
      return;
    }

    var containerSize = containerInventory.getSize();

    for (var index = 0; index < containerSize; ++index)
      inventory.setItem(index, containerInventory.getItem(index));
  }

  @Override
  public void show() {
    super.show();

    if (displayData.instance() != null && displayData.instance().chatMessage != null) {
      displayData.instance().chatMessage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("player", player.getName())
      );
    }
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    var title = (
      displayData.instance() == null || displayData.instance().inventoryTitle == null
        ? config.rootSection.mechanic.showcase.defaultInventoryTitle
        : displayData.instance().inventoryTitle
    ).interpret(SlotType.INVENTORY_TITLE, null).getFirst();

    var containerInventory = getContainerInventory();

    if (containerInventory == null)
      return new DisplayInventoryParameters(title, InventoryType.CHEST, 9 * 3);

    return new DisplayInventoryParameters(title, containerInventory.getType(), containerInventory.getSize());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private @Nullable Inventory getContainerInventory() {
    if (displayData.instance() == null || displayData.instance().containerPosition == null)
      return null;

    var containerBlock = displayData.instance().containerPosition.getBlock();

    if (containerBlock.getState(false) instanceof Container container)
      return container.getInventory();

    return null;
  }
}

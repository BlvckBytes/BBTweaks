package me.blvckbytes.bbtweaks.mechanic.showcase;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.Display;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class ShowcaseDisplay extends Display<ShowcaseDisplayData> {

  public ShowcaseDisplay(
    Player player,
    ShowcaseDisplayData displayData,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    show();
  }

  @Override
  protected void renderItems() {}

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
  protected Inventory makeInventory() {
    var title = (
      displayData.instance() == null || displayData.instance().inventoryTitle == null
        ? config.rootSection.mechanic.showcase.defaultInventoryTitle
        : displayData.instance().inventoryTitle
    ).interpret(SlotType.INVENTORY_TITLE, null).getFirst();

    Inventory inventory;

    if (displayData.instance() == null || displayData.instance().containerPosition == null || !(displayData.instance().containerPosition.getBlock().getState(false) instanceof Container container)) {
      inventory = Bukkit.createInventory(null, 9 * 3, title);
      inventory.setItem(13, displayData.frameItem());
      return inventory;
    }

    var containerInventory = container.getInventory();
    var containerSize = containerInventory.getSize();

    if (containerInventory.getType() == InventoryType.CHEST)
      inventory = Bukkit.createInventory(null, containerSize, title);
    else
      inventory = Bukkit.createInventory(null, containerInventory.getType(), title);

    for (var index = 0; index < containerSize; ++index)
      inventory.setItem(index, containerInventory.getItem(index));

    return inventory;
  }

  @Override
  public void onConfigReload() {
    show();
  }
}

package me.blvckbytes.bbtweaks.itemdata.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.itemdata.ItemDataAccessor;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.EmptyObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class ItemDataDisplay extends Display<EmptyObject> {

  private final boolean isFloodgate;

  public ItemDataDisplay(
    Player player,
    EmptyObject displayData,
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    isFloodgate = floodgateIntegration.isFloodgatePlayer(player);

    show();
  }

  @Override
  protected void renderItems() {
    var storageContents = player.getInventory().getStorageContents();
    var displaySize = inventory.getSize();

    for (var inventoryIndex = 0; inventoryIndex < storageContents.length; ++inventoryIndex) {
      if (inventoryIndex >= displaySize)
        break;

      // Account for how the hotbar is really the first row, as to make both
      // the top- and bottom inventory look exactly alike.
      var displayIndex = (inventoryIndex + 9 * 3) % displaySize;

      var storageItem = storageContents[inventoryIndex];

      if (storageItem == null || storageItem.getType().isAir()) {
        inventory.setItem(displayIndex, null);
        continue;
      }

      var displayItem = new ItemStack(storageItem);
      var itemEnvironment = ItemDataAccessor.makeEnvironmentIfHasData(storageItem);

      if (itemEnvironment == null)
        itemEnvironment = new InterpretationEnvironment();

      config.rootSection.itemData.infoDisplay.items.itemPatch.patch(
        displayItem,
        itemEnvironment
          .withVariable("is_floodgate", isFloodgate)
      );

      inventory.setItem(displayIndex, displayItem);
    }
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.itemData.infoDisplay.createInventory(makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("player", player.getName());
  }
}

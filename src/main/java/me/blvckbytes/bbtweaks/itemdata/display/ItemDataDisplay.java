package me.blvckbytes.bbtweaks.itemdata.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.gui.GuiItemStackSection;
import at.blvckbytes.cm_mapper.section.gui.ItemConsumer;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.itemdata.ItemDataAccessor;
import me.blvckbytes.bbtweaks.util.Display;
import me.blvckbytes.bbtweaks.util.DisplayInventoryParameters;
import me.blvckbytes.bbtweaks.util.EmptyObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

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
  }

  @Override
  protected void renderItems(ItemConsumer itemConsumer) {
    var storageContents = player.getInventory().getStorageContents();

    for (var inventoryIndex = 0; inventoryIndex < storageContents.length; ++inventoryIndex) {
      // Account for how the hotbar is really the first row, as to make both
      // the top- and bottom inventory look exactly alike.
      var displayIndex = (inventoryIndex + 9 * 3) % storageContents.length;

      var storageItem = storageContents[inventoryIndex];

      if (storageItem == null || storageItem.getType().isAir()) {
        itemConsumer.handle(displayIndex, null);
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

      itemConsumer.handle(displayIndex, displayItem);
    }
  }

  @Override
  protected @Nullable GuiItemStackSection getFillerItem() {
    return null;
  }

  @Override
  protected DisplayInventoryParameters makeInventoryParameters() {
    return DisplayInventoryParameters.fromSection(config.rootSection.itemData.infoDisplay, makeEnvironment());
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

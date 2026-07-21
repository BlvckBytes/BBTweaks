package me.blvckbytes.bbtweaks.itemdata.display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegrationLoader;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import me.blvckbytes.bbtweaks.util.EmptyObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public class ItemDataDisplayHandler extends DisplayHandler<ItemDataDisplay, EmptyObject> {

  private final FloodgateIntegration floodgateIntegration;

  public ItemDataDisplayHandler(
    FloodgateIntegrationLoader floodgateIntegrationLoader,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, ItemDataDisplay.class);

    this.floodgateIntegration = floodgateIntegrationLoader.floodgateIntegration;
  }

  @Override
  public ItemDataDisplay instantiateDisplay(Player player, EmptyObject displayData) {
    return new ItemDataDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, ItemDataDisplay display, ClickType clickType, int slot) {
    config.rootSection.itemData.cannotModifyDisplay.sendMessage(player);
  }

  @Override
  protected void handleOwnInventoryClick(Player player, ItemDataDisplay display, ClickType clickType, int slot) {
    Bukkit.getScheduler().runTaskLater(plugin, display::renderItems, 1);
  }

  @Override
  protected void handleOwnInventoryDrag(Player player, ItemDataDisplay display, Set<Integer> slots) {
    Bukkit.getScheduler().runTaskLater(plugin, display::renderItems, 1);
  }
}

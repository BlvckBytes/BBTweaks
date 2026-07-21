package me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.hotbar_randomizer.HotbarRandomizerSettings;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.util.DisplayHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class HotbarRandomizerSettingsDisplayHandler extends DisplayHandler<HotbarRandomizerSettingsDisplay, HotbarRandomizerSettings> {

  private final FloodgateIntegration floodgateIntegration;

  public HotbarRandomizerSettingsDisplayHandler(
    FloodgateIntegration floodgateIntegration,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(config, plugin, HotbarRandomizerSettingsDisplay.class);

    this.floodgateIntegration = floodgateIntegration;
  }

  @Override
  public HotbarRandomizerSettingsDisplay instantiateDisplay(Player player, HotbarRandomizerSettings displayData) {
    return new HotbarRandomizerSettingsDisplay(player, displayData, floodgateIntegration, config, plugin);
  }

  @Override
  protected void handleClick(Player player, HotbarRandomizerSettingsDisplay display, ClickType clickType, int slot) {
    if (clickType != ClickType.LEFT)
      return;

    if (config.rootSection.hotbarRandomizer.settingsDisplay.items.enabled.getDisplaySlots().contains(slot)) {
      display.displayData.setEnabledAndSendMessage(null);
      display.renderItems();
      return;
    }

    var slots = config.rootSection.hotbarRandomizer.settingsDisplay.items.enabledSlot.getDisplaySlots();
    var slotIndex = slots.indexOf(slot);

    if (slotIndex < 0)
      return;

    display.displayData.toggleSlotEnableState(slotIndex);
    display.renderItems();
  }

  @EventHandler
  public void onSlotChange(PlayerInventorySlotChangeEvent event) {
    var display = getDisplay(event.getPlayer());

    if (display == null)
      return;

    if (event.getRawSlot() < display.getSize() + 9 * 3)
      return;

    Bukkit.getScheduler().runTaskLater(plugin, display::renderItems, 1L);
  }
}

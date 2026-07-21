package me.blvckbytes.bbtweaks.util;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class DisplayHandler<DisplayType extends Display<DisplayDataType>, DisplayDataType> implements Listener, Disableable {

  // If players move to their own inventory and close the UI quickly enough, the server will send back a packet
  // undoing that slot which assumed the top-inventory to still be open, and thus the undo won't work. For survival,
  // it's merely cosmetic, but for creative, the client will actually call this item into existence. While not
  // necessarily critical, it just makes the plugin look bad. On closing the inventory, if the last move happened
  // within a certain threshold of time, let's just update the player's inventory, as to make that ghost-item vanish.
  private static final long MOVE_GHOST_ITEM_THRESHOLD_MS = 500;

  protected final ConfigKeeper<MainSection> config;
  protected final Plugin plugin;
  private final Class<DisplayType> displayTypeClass;

  private final Map<UUID, Long> lastMoveToOwnInventoryStampByPlayerId;

  protected DisplayHandler(
    ConfigKeeper<MainSection> config,
    Plugin plugin,
    Class<DisplayType> displayTypeClass
  ) {
    this.lastMoveToOwnInventoryStampByPlayerId = new HashMap<>();
    this.config = config;
    this.plugin = plugin;
    this.displayTypeClass = displayTypeClass;
  }

  public abstract DisplayType instantiateDisplay(Player player, DisplayDataType displayData);

  public @Nullable DisplayType getDisplay(Player player) {
    if (!(player.getOpenInventory().getTopInventory().getHolder(false) instanceof Display<?> display))
      return null;

    if (!displayTypeClass.isInstance(display))
      return null;

    return displayTypeClass.cast(display);
  }

  public void show(Player player, DisplayDataType displayData) {
    // TODO: We should really call show here and not rely on show() being present in the display-constructor
    instantiateDisplay(player, displayData);
  }

  public void reopen(DisplayType display) {
    // TODO: This could really be replaced with just an invocation of show() at all call-sites
    display.show();
  }

  public void closeIfOpen(Player player) {
    if (getDisplay(player) != null)
      player.closeInventory();
  }

  protected abstract void handleClick(Player player, DisplayType display, ClickType clickType, int slot);

  protected void handleOwnInventoryClick(Player player, DisplayType display, ClickType clickType, int slot) {}

  protected void handleOwnInventoryDrag(Player player, DisplayType display, Set<Integer> slots) {}

  @Override
  public void disable() {
    for (var player : Bukkit.getOnlinePlayers()) {
      if (getDisplay(player) != null)
        player.closeInventory();
    }
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    for (var player : Bukkit.getOnlinePlayers()) {
      var display = getDisplay(player);

      if (display != null)
        display.onConfigReload();
    }
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var display = getDisplay(player);

    if (display == null)
      return;

    event.setCancelled(true);

    if (!display.isInventory(player.getOpenInventory().getTopInventory()))
      return;

    var displaySize = display.getSize();
    var rawSlots = event.getRawSlots();

    for (var rawSlot : rawSlots) {
      // Affected the top-inventory
      if (rawSlot < displaySize)
        return;
    }

    // Allow to drag exclusively in the bottom-inventory
    event.setCancelled(false);

    handleOwnInventoryDrag(player, display, event.getInventorySlots());
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player))
      return;

    var display = getDisplay(player);

    // Only remove on inventory match, as to prevent removal on title update
    if (display != null && display.isInventory(event.getInventory())) {
      var lastMoveToOwnInventoryStamp = lastMoveToOwnInventoryStampByPlayerId.remove(player.getUniqueId());

      if (
        lastMoveToOwnInventoryStamp != null &&
        System.currentTimeMillis() - lastMoveToOwnInventoryStamp < MOVE_GHOST_ITEM_THRESHOLD_MS
      ) {
        Bukkit.getScheduler().runTask(plugin, player::updateInventory);
      }
    }
  }

  @EventHandler
  public void onCreative(InventoryCreativeEvent event) {
    handleInventoryClick(event);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    handleInventoryClick(event);
  }

  private void handleInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var display = getDisplay(player);

    if (display == null)
      return;

    event.setCancelled(true);

    var action = event.getAction();

    if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.getClickedInventory() != player.getInventory())
      lastMoveToOwnInventoryStampByPlayerId.put(player.getUniqueId(), System.currentTimeMillis());

    if (!display.isInventory(player.getOpenInventory().getTopInventory()))
      return;

    var clickType = event.getClick();
    var slot = event.getRawSlot();

    // Clicked somewhere outside the inventory
    if (slot < 0) {
      if (action == InventoryAction.DROP_ONE_CURSOR || action == InventoryAction.DROP_ALL_CURSOR)
        event.setCancelled(false);

      return;
    }

    var displaySize = display.getSize();

    // Clicked somewhere inside own inventory
    if (slot >= displaySize) {
      var inventoryRelativeSlot = slot - displaySize;

      if (inventoryRelativeSlot >= 9 * 4)
        return;

      if (isAllowedActionInOwnInventory(action))
        event.setCancelled(false);

      handleOwnInventoryClick(player, display, clickType, inventoryRelativeSlot);
      return;
    }

    handleClick(player, display, clickType, slot);
  }

  private boolean isAllowedActionInOwnInventory(InventoryAction action) {
    return switch (action) {
      case
        PICKUP_ALL,
        PICKUP_SOME,
        PICKUP_HALF,
        PICKUP_ONE,
        PLACE_ALL,
        PLACE_SOME,
        PLACE_ONE,
        SWAP_WITH_CURSOR,
        DROP_ALL_SLOT,
        DROP_ONE_SLOT,
        HOTBAR_SWAP,
        CLONE_STACK,
        PICKUP_FROM_BUNDLE,
        PICKUP_ALL_INTO_BUNDLE,
        PICKUP_SOME_INTO_BUNDLE,
        PLACE_FROM_BUNDLE,
        PLACE_ALL_INTO_BUNDLE,
        PLACE_SOME_INTO_BUNDLE -> true;
      default -> false;
    };
  }
}

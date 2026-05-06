package me.blvckbytes.bbtweaks.shulker_accessor;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.shulker_accessor.change_detection.InventoryChangeDetector;
import me.blvckbytes.bbtweaks.shulker_accessor.change_detection.InventoryChangedEvent;
import me.blvckbytes.bbtweaks.shulker_accessor.change_detection.InventoryInvalidatedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Predicate;

public class ShulkerAccessorListener implements Listener {

  private final Plugin plugin;
  private final InventoryChangeDetector changeDetector;
  private final ConfigKeeper<MainSection> config;

  private final Object2LongMap<UUID> lastShulkerAccessStampByPlayerId;

  private long relativeTime;

  public ShulkerAccessorListener(
    Plugin plugin,
    InventoryChangeDetector changeDetector,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.changeDetector = changeDetector;
    this.config = config;

    this.lastShulkerAccessStampByPlayerId = new Object2LongOpenHashMap<>();
    this.lastShulkerAccessStampByPlayerId.defaultReturnValue(0);

    Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> ++relativeTime, 0, 0);
  }

  @EventHandler
  public void onInventoryInvalidate(InventoryInvalidatedEvent event) {
    if (event.getHolder() instanceof ShulkerAccessorHolder holder)
      Bukkit.getScheduler().runTaskLater(plugin, holder::closeAll, 1L);
  }

  @EventHandler
  public void onInventoryChange(InventoryChangedEvent event) {
    if (!(event.getHolder() instanceof ShulkerAccessorHolder holder))
      return;

    if (!holder.tryWriteBackToItem())
      Bukkit.getScheduler().runTaskLater(plugin, holder::closeAll, 1L);
  }

  @EventHandler
  public void onDropItem(PlayerDropItemEvent event) {
    var player = event.getPlayer();
    var playerInventory = player.getInventory();

    playerInventory.getHeldItemSlot();

    if (doesAnyAccessorHolderMatch(it -> it.isShulkerItemContainedByInventoryAtSlot(playerInventory, playerInventory.getHeldItemSlot())))
      event.setCancelled(true);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR)
      return;

    var player = event.getPlayer();
    var playerInventory = player.getInventory();

    var itemSlot = playerInventory.getHeldItemSlot();

    var newHolder = ShulkerAccessorHolder.instantiateIfValidShulker(
      playerInventory.getItemInMainHand(),
      playerInventory,
      itemSlot,
      null,
      config
    );

    if (newHolder == null)
      return;

    if (doesAnyAccessorHolderMatch(otherHolder -> otherHolder.isShulkerItemContainedByInventoryAtSlot(playerInventory, itemSlot))) {
      event.setCancelled(true);
      return;
    }

    if (touchCooldownAndGetIfStillActive(player))
      return;

    event.setCancelled(true);

    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(newHolder.getInventory()), 1L);
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player))
      return;

    var slotType = event.getSlotType();

    if (slotType != InventoryType.SlotType.CONTAINER && slotType != InventoryType.SlotType.QUICKBAR)
      return;

    var clickedInventory = event.getClickedInventory();

    if (clickedInventory == null)
      return;

    var clickedItem = event.getCurrentItem();

    if (clickedItem == null)
      return;

    var itemSlot = event.getSlot();

    if (itemSlot < 0 || itemSlot >= clickedInventory.getSize())
      return;

    var newHolder = ShulkerAccessorHolder.instantiateIfValidShulker(
      clickedItem,
      clickedInventory,
      itemSlot,
      player.getOpenInventory().title(),
      config
    );

    if (newHolder == null)
      return;

    if (doesAnyAccessorHolderMatch(otherHolder -> otherHolder.isShulkerItemContainedByInventoryAtSlot(clickedInventory, itemSlot) || otherHolder.sharesBlocksWith(newHolder))) {
      event.setCancelled(true);
      return;
    }

    if (event.getClick() != ClickType.SHIFT_RIGHT)
      return;

    if (touchCooldownAndGetIfStillActive(player))
      return;

    event.setCancelled(true);

    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(newHolder.getInventory()), 1L);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onTeleport(PlayerTeleportEvent event) {
    var player = event.getPlayer();

    if (player.getOpenInventory().getTopInventory().getHolder() instanceof ShulkerAccessorHolder)
      Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 1L);
  }

  private boolean touchCooldownAndGetIfStillActive(Player player) {
    var playerId = player.getUniqueId();
    var lastAccessStamp = lastShulkerAccessStampByPlayerId.getLong(playerId);

    if (relativeTime - lastAccessStamp <= config.rootSection.shulkerAccessor.openShulkerCooldownTicks)
      return true;

    lastShulkerAccessStampByPlayerId.put(playerId, relativeTime);
    return false;
  }

  private boolean doesAnyAccessorHolderMatch(Predicate<ShulkerAccessorHolder> predicate) {
    for (var observedHolder : changeDetector.getObservedHolders()) {
      if (!(observedHolder instanceof ShulkerAccessorHolder holder))
        continue;

      if (predicate.test(holder))
        return true;
    }

    return false;
  }
}

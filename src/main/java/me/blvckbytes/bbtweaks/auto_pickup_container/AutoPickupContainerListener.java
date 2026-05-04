package me.blvckbytes.bbtweaks.auto_pickup_container;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoPickupContainerListener implements Listener {

  // TODO: Feature-request by a user: pick up items into containers even if the inv is full

  private final NamespacedKey containerMarkerKey;
  private final NamespacedKey placedItemKey;

  private final Map<UUID, PickupTickWindow> pickupTickWindowByPlayerId;

  public AutoPickupContainerListener(Plugin plugin) {
    this.containerMarkerKey = new NamespacedKey(plugin, "auto-pickup-container");
    this.placedItemKey = new NamespacedKey(plugin, "auto-pickup-placed-item");

    this.pickupTickWindowByPlayerId = new HashMap<>();

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      pickupTickWindowByPlayerId.values().forEach(this::processPickupTickWindow);
      pickupTickWindowByPlayerId.clear();
    }, 0L, 0L);
  }

  private void processPickupTickWindow(PickupTickWindow window) {
    if (!window.player.isOnline())
      return;

    var session = new InventoryManipulationSession(window.player);

    for (var itemBucket : window.buckets) {
      var remainingAmountToReduce = session.tryAddItemToContainersAndGetAddedAmount(itemBucket.item, itemBucket.getTotalCount());

      for (var slotIndex = 0; slotIndex < ItemBucket.INVENTORY_SIZE; ++slotIndex) {
        if (remainingAmountToReduce <= 0)
          break;

        var pickedUpCount = itemBucket.getPickedUpCountForSlot(slotIndex);

        if (pickedUpCount <= 0)
          continue;

        var amountToReduce = Math.min(pickedUpCount, remainingAmountToReduce);

        session.reduceItemInPlayerInventoryBy(slotIndex, amountToReduce);

        remainingAmountToReduce -= amountToReduce;
      }
    }

    session.onCompletion();
  }

  public @Nullable MarkerModifyError modifyItemToBecomeAutoPickupContainer(ItemStack item) {
    if (!Tag.SHULKER_BOXES.isTagged(item.getType()))
      return MarkerModifyError.WRONG_ITEM_TYPE;

    var meta = Objects.requireNonNull(item.getItemMeta());
    var pdc = meta.getPersistentDataContainer();

    var existingValue = pdc.get(containerMarkerKey, PersistentDataType.BOOLEAN);

    if (existingValue != null && existingValue)
      return MarkerModifyError.ALREADY_MARKED;

    pdc.set(containerMarkerKey, PersistentDataType.BOOLEAN, true);
    item.setItemMeta(meta);

    return null;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPlace(BlockPlaceEvent event) {
    if (!(event.getBlockPlaced().getState() instanceof ShulkerBox shulkerBox))
      return;

    var placedItem = event.getItemInHand();

    if (!Tag.SHULKER_BOXES.isTagged(placedItem.getType()))
      return;

    if (!doesContainMarker(placedItem.getPersistentDataContainer()))
      return;

    shulkerBox.getPersistentDataContainer().set(placedItemKey, PersistentDataType.BYTE_ARRAY, placedItem.serializeAsBytes());
    shulkerBox.update(true, false);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onBlockDropItem(BlockDropItemEvent event) {
    if (!(event.getBlockState() instanceof ShulkerBox shulkerBox))
      return;

    var placedItemBytes = shulkerBox.getPersistentDataContainer().get(placedItemKey, PersistentDataType.BYTE_ARRAY);

    if (placedItemBytes == null)
      return;

    Item droppedItem = null;
    ItemStack droppedStack = null;

    for (var item : event.getItems()) {
      droppedStack = item.getItemStack();

      if (!Tag.SHULKER_BOXES.isTagged(droppedStack.getType()))
        continue;

      if (droppedItem != null)
        return;

      droppedItem = item;
    }

    if (droppedItem == null)
      return;

    var placedItem = ItemStack.deserializeBytes(placedItemBytes);

    var placedMeta = placedItem.getItemMeta();
    var droppedMeta = droppedStack.getItemMeta();

    if (placedMeta == null || droppedMeta == null)
      return;

    droppedMeta.displayName(placedMeta.displayName());
    droppedMeta.lore(placedMeta.lore());

    droppedMeta.getPersistentDataContainer().set(containerMarkerKey, PersistentDataType.BOOLEAN, true);

    droppedStack.setItemMeta(droppedMeta);
    droppedItem.setItemStack(droppedStack);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var pickedUpItem = event.getItem().getItemStack();

    if (Tag.SHULKER_BOXES.isTagged(pickedUpItem.getType()))
      return;

    pickupTickWindowByPlayerId
      .computeIfAbsent(player.getUniqueId(), k -> new PickupTickWindow(player))
      .accessOrCreateBucket(pickedUpItem)
      .analyzePickupSlots(pickedUpItem, player.getInventory());
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean doesContainMarker(PersistentDataContainerView pdc) {
    var markerFlag = pdc.get(containerMarkerKey, PersistentDataType.BOOLEAN);
    return markerFlag != null && markerFlag;
  }
}

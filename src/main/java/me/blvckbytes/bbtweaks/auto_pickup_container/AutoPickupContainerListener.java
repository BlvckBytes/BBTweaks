package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AutoPickupContainerListener implements Listener {

  private final NamespacedKey containerMarkerKey;

  public AutoPickupContainerListener(Plugin plugin) {
    this.containerMarkerKey = new NamespacedKey(plugin, "auto-pickup-container");
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
  public void onPickup(EntityPickupItemEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var playerInventory = player.getInventory();
    var pickedUpItem = event.getItem().getItemStack();

    for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
      var possibleContainerItem = playerInventory.getItem(slotIndex);

      if (possibleContainerItem == null)
        continue;

      var remainingAmount = tryAddItemAndGetRemainingAmount(possibleContainerItem, pickedUpItem);

      pickedUpItem.setAmount(remainingAmount);

      if (remainingAmount <= 0)
        break;
    }

    event.getItem().setItemStack(pickedUpItem);
  }

  private int tryAddItemAndGetRemainingAmount(ItemStack containerItem, ItemStack itemToAdd) {
    if (!Tag.SHULKER_BOXES.isTagged(containerItem.getType()))
      return itemToAdd.getAmount();

    var containerMeta = containerItem.getItemMeta();

    if (containerMeta == null)
      return itemToAdd.getAmount();

    var pdc = containerMeta.getPersistentDataContainer();
    var markerFlag = pdc.get(containerMarkerKey, PersistentDataType.BOOLEAN);

    if (markerFlag == null || !markerFlag)
      return itemToAdd.getAmount();

    if (!(containerMeta instanceof BlockStateMeta blockStateMeta))
      return itemToAdd.getAmount();

    if (!(blockStateMeta.getBlockState() instanceof Container container))
      return itemToAdd.getAmount();

    var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(itemToAdd, container.getInventory());

    if (remainingAmount != itemToAdd.getAmount()) {
      blockStateMeta.setBlockState(container);
      containerItem.setItemMeta(containerMeta);
    }

    return remainingAmount;
  }
}

package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class AddToContainerSession {

  private final PlayerInventory inventory;
  private final ItemStack[] storageContents;

  private final List<LazyContainer> containers;

  private boolean dirty;

  public AddToContainerSession(
    Player player,
    FilterPredicateAccessor filterPredicateAccessor,
    InventoryItemPredicate shulkerPredicate
  ) {
    this.inventory = player.getInventory();
    this.storageContents = inventory.getStorageContents();
    this.containers = new ArrayList<>();

    for (var slotIndex = 0; slotIndex < storageContents.length; ++slotIndex) {
      var item = storageContents[slotIndex];

      if (item == null || item.getType().isAir())
        continue;

      if (!Tag.SHULKER_BOXES.isTagged(item.getType()))
        continue;

      if (!shulkerPredicate.test(inventory, slotIndex, item))
        continue;

      containers.add(new LazyContainer(player, item, filterPredicateAccessor));
    }
  }

  public int tryAddItemToContainersAndGetAddedAmount(ItemStack itemToAdd) {
    var amount = itemToAdd.getAmount();
    var remainingAmount = amount;

    for (var container : containers) {
      var addedAmount = container.tryAddItemAndGetAddedAmount(itemToAdd, remainingAmount);

      if (addedAmount <= 0)
        continue;

      dirty = true;

      remainingAmount -= addedAmount;

      if (remainingAmount <= 0)
        break;
    }

    return amount - remainingAmount;
  }

  public void onCompletion(ContainerWritebackHandler writebackHandler) {
    if (!dirty)
      return;

    for (var container : containers)
      container.onCompletion(writebackHandler);

    inventory.setStorageContents(storageContents);
  }
}

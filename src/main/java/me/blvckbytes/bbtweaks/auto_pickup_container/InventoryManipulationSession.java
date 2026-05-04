package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class InventoryManipulationSession {

  private final PlayerInventory inventory;
  private final ItemStack[] storageContents;

  private final List<LazyContainer> containers;

  private boolean dirty;

  public InventoryManipulationSession(Player player, Predicate<ItemStack> containerPredicate) {
    this.inventory = player.getInventory();
    this.storageContents = inventory.getStorageContents();
    this.containers = new ArrayList<>();

    for (var item : storageContents) {
      if (item == null || item.getType().isAir())
        continue;

      if (!Tag.SHULKER_BOXES.isTagged(item.getType()))
        continue;

      if (!containerPredicate.test(item))
        continue;

      containers.add(new LazyContainer(item));
    }
  }

  public void reduceItemInPlayerInventoryBy(int slot, int amountToTake) {
    var targetItem = storageContents[slot];

    if (targetItem == null || targetItem.getType().isAir())
      return;

    dirty = true;

    var existingAmount = targetItem.getAmount();

    if (existingAmount <= amountToTake) {
      storageContents[slot] = null;
      return;
    }

    targetItem.setAmount(existingAmount - amountToTake);
  }

  public int tryAddItemToContainersAndGetAddedAmount(ItemStack itemToAdd, int amount) {
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

  public void onCompletion() {
    if (!dirty)
      return;

    containers.forEach(LazyContainer::onCompletion);

    inventory.setStorageContents(storageContents);
  }
}

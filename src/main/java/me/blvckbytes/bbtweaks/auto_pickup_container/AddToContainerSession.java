package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
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
    ShulkerPredicate shulkerPredicate
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

      var disableReason = shulkerPredicate.test(inventory, slotIndex, item);

      containers.add(new LazyContainer(player, item, filterPredicateAccessor, disableReason));
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

  public UsageCounts calculateUsageCounts() {
    var usedSlotCount = 0;
    var vacantSlotCount = 0;
    var containerCount = 0;

    for (var container : containers) {
      var usageCounts = container.getUsageCounts();

      usedSlotCount += IntTuple.getFirst(usageCounts);
      vacantSlotCount += IntTuple.getSecond(usageCounts);

      ++containerCount;
    }

    return new UsageCounts(usedSlotCount, vacantSlotCount, containerCount);
  }

  public void onCompletion() {
    if (!dirty)
      return;

    for (var container : containers)
      container.onCompletion();

    inventory.setStorageContents(storageContents);
  }
}

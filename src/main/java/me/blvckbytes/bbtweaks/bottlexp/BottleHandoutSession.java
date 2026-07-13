package me.blvckbytes.bbtweaks.bottlexp;

import me.blvckbytes.bbtweaks.auto_pickup_container.AddFlag;
import me.blvckbytes.bbtweaks.auto_pickup_container.AddToContainerSession;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.util.SimulatingAddOnlyInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BottleHandoutSession {

  private static final ItemStack BOTTLE_ITEM = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

  private final Player player;

  private int inventoryBottlesCount;

  private final @Nullable SimulatingAddOnlyInventory simulatingPlayerInventory;
  private final @Nullable AddToContainerSession addToContainerSession;

  public BottleHandoutSession(
    Player player,
    BottleStorage bottleStorage,
    AutoPickupContainerListener autoPickupContainerListener
  ) {
    this.player = player;

    this.simulatingPlayerInventory = bottleStorage.intoInventory ? new SimulatingAddOnlyInventory(player.getInventory(), null, null) : null;
    this.addToContainerSession = bottleStorage.intoShulkers ? autoPickupContainerListener.makePickupSession(player) : null;
  }

  public boolean encounteredShulkerBoxes() {
    return addToContainerSession != null && addToContainerSession.foundContainers();
  }

  public boolean tryAddABottle() {
    if (addToContainerSession != null) {
      if (addToContainerSession.tryAddItemToContainersAndGetAddedAmount(BOTTLE_ITEM, AddFlag.ALLOW_UNMARKED) > 0)
        return true;
    }

    if (simulatingPlayerInventory != null) {
      var addedAmount = simulatingPlayerInventory.addItemAndGetAddedAmount(Material.EXPERIENCE_BOTTLE, 1);

      if (addedAmount == 0)
        return false;

      ++inventoryBottlesCount;
      return true;
    }

    return false;
  }

  public void onCompletion() {
    if (addToContainerSession != null)
      addToContainerSession.onCompletion();

    var remainingBottleCount = inventoryBottlesCount;

    var playerInventory = player.getInventory();

    while (remainingBottleCount > 0) {
      var dropCount = Math.min(remainingBottleCount, Material.EXPERIENCE_BOTTLE.getMaxStackSize());

      playerInventory
        .addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, dropCount))
        .values()
        .forEach(player::dropItem);

      remainingBottleCount -= dropCount;
    }
  }
}

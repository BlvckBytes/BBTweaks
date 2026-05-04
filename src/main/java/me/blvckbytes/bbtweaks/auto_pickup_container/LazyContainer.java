package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class LazyContainer {

  private final ItemStack itemStack;

  private boolean dirty;

  private boolean inaccessible;
  private @Nullable BlockStateMeta blockStateMeta;
  private @Nullable Container container;
  private @Nullable Inventory inventory;

  public LazyContainer(ItemStack itemStack) {
    this.itemStack = itemStack;
  }

  public void onCompletion() {
    if (!dirty || container == null || blockStateMeta == null)
      return;

    blockStateMeta.setBlockState(container);
    itemStack.setItemMeta(blockStateMeta);
  }

  public int tryAddItemAndGetAddedAmount(ItemStack itemToAdd, int amount) {
    if (inventory == null) {
      if (inaccessible)
        return 0;

      if (!(itemStack.getItemMeta() instanceof BlockStateMeta _blockStateMeta)) {
        inaccessible = true;
        return 0;
      }

      if (!(_blockStateMeta.getBlockState() instanceof Container _container)) {
        inaccessible = true;
        return 0;
      }

      blockStateMeta = _blockStateMeta;
      container = _container;
      inventory = _container.getInventory();
    }

    var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(itemToAdd, amount, inventory);

    if (remainingAmount < amount)
      dirty = true;

    return amount - remainingAmount;
  }
}

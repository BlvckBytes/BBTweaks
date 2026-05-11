package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class LazyContainer {

  private final Player player;
  private final ItemStack itemStack;
  private final FilterPredicateAccessor filterPredicateAccessor;

  private boolean dirty;

  private boolean inaccessible;
  private @Nullable BlockStateMeta blockStateMeta;
  private @Nullable Container container;
  private @Nullable Inventory inventory;
  private @Nullable ItemPredicate filter;

  public LazyContainer(
    Player player,
    ItemStack itemStack,
    FilterPredicateAccessor filterPredicateAccessor
  ) {
    this.player = player;
    this.itemStack = itemStack;
    this.filterPredicateAccessor = filterPredicateAccessor;
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
      filter = filterPredicateAccessor.accessFilterPredicate(player, _blockStateMeta.getPersistentDataContainer());
    }

    if (filter != null && !filter.test(itemToAdd))
      return 0;

    var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(itemToAdd, amount, inventory);

    if (remainingAmount < amount)
      dirty = true;

    return amount - remainingAmount;
  }
}

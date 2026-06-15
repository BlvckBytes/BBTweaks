package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Tag;
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
  private final @Nullable DisableReason disableReason;

  private boolean dirty;

  private boolean inaccessible;
  private @Nullable BlockStateMeta blockStateMeta;
  private @Nullable Container container;
  private @Nullable Inventory inventory;
  private @Nullable ItemPredicate filter;

  public LazyContainer(
    Player player,
    ItemStack itemStack,
    FilterPredicateAccessor filterPredicateAccessor,
    @Nullable DisableReason disableReason
  ) {
    this.player = player;
    this.itemStack = itemStack;
    this.filterPredicateAccessor = filterPredicateAccessor;
    this.disableReason = disableReason;
  }

  public void onCompletion(ContainerWritebackHandler writebackHandler) {
    if (!dirty || container == null || blockStateMeta == null)
      return;

    blockStateMeta.setBlockState(container);
    writebackHandler.handle(itemStack, blockStateMeta);
    itemStack.setItemMeta(blockStateMeta);
  }

  public int tryAddItemAndGetAddedAmount(ItemStack itemToAdd, int amount) {
    if (disableReason != null || Tag.SHULKER_BOXES.isTagged(itemToAdd.getType()))
      return 0;

    tryAccessInventory();

    if (inventory == null)
      return 0;

    if (filter != null && !filter.test(itemToAdd))
      return 0;

    var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(itemToAdd, amount, inventory);

    if (remainingAmount < amount)
      dirty = true;

    return amount - remainingAmount;
  }

  public long getUsageCounts() {
    if (disableReason == DisableReason.NOT_MARKED)
      return 0;

    tryAccessInventory();

    if (inventory == null)
      return 0;

    var size = inventory.getSize();
    var vacantSlotCount = 0;

    for (var index = 0; index < size; ++index) {
      var item = inventory.getItem(index);

      if (item == null || item.getType().isAir() || item.getAmount() == 0)
        ++vacantSlotCount;
    }

    return IntTuple.create(size - vacantSlotCount, vacantSlotCount);
  }

  private void tryAccessInventory() {
    if (inventory == null) {
      if (inaccessible)
        return;

      if (!(itemStack.getItemMeta() instanceof BlockStateMeta _blockStateMeta)) {
        inaccessible = true;
        return;
      }

      if (!(_blockStateMeta.getBlockState() instanceof Container _container)) {
        inaccessible = true;
        return;
      }

      blockStateMeta = _blockStateMeta;
      container = _container;
      inventory = _container.getInventory();
      filter = filterPredicateAccessor.accessFilterPredicate(player, _blockStateMeta.getPersistentDataContainer());
    }
  }
}

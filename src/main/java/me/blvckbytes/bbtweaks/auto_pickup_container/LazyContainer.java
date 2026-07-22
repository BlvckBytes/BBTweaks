package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.mechanic.util.InventoryUtil;
import me.blvckbytes.bbtweaks.util.SimulatingAddOnlyInventory;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class LazyContainer {

  private final @Nullable Player player;
  private final ItemStack itemStack;
  private final @Nullable FilterPredicateAccessor filterPredicateAccessor;
  private final EnumSet<DisableReason> disableReasons;

  private boolean dirty;

  private boolean inaccessible;
  private @Nullable BlockStateMeta blockStateMeta;
  private @Nullable Container container;
  private @Nullable Inventory inventory;
  private @Nullable ItemPredicate filter;

  public LazyContainer(
    @Nullable Player player,
    ItemStack itemStack,
    @Nullable FilterPredicateAccessor filterPredicateAccessor,
    EnumSet<DisableReason> disableReasons
  ) {
    this.player = player;
    this.itemStack = itemStack;
    this.filterPredicateAccessor = filterPredicateAccessor;
    this.disableReasons = disableReasons;
  }

  public static @Nullable Inventory tryAccessInventory(ItemStack itemStack) {
    var instance = new LazyContainer(null, itemStack, null, null);
    instance.tryAccessInventory();
    return instance.inventory;
  }

  public void onCompletion() {
    if (!dirty || container == null || blockStateMeta == null || inventory == null)
      return;

    blockStateMeta.setBlockState(container);
    itemStack.setItemMeta(blockStateMeta);
  }

  public int tryAddItemAndGetAddedAmount(ItemStack itemToAdd, int amount, EnumSet<AddFlag> flags) {
    if (disableReasons.contains(DisableReason.CURRENTLY_VIEWED))
      return 0;

    if (disableReasons.contains(DisableReason.NOT_MARKED)) {
      if (!flags.contains(AddFlag.ALLOW_UNMARKED))
        return 0;
    }

    if (Tag.SHULKER_BOXES.isTagged(itemToAdd.getType()))
      return 0;

    tryAccessInventory();

    if (inventory == null)
      return 0;

    if (filter != null && !filter.test(itemToAdd))
      return 0;

    if (flags.contains(AddFlag.DRY_RUN))
      return new SimulatingAddOnlyInventory(inventory, null, null).addItemAndGetAddedAmount(itemToAdd, amount);

    var remainingAmount = InventoryUtil.addItemToInventoryAndGetRemainingAmount(itemToAdd, amount, inventory);

    if (remainingAmount < amount)
      dirty = true;

    return amount - remainingAmount;
  }

  public long calculateUsageCountsIfMarked() {
    if (disableReasons.contains(DisableReason.NOT_MARKED))
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

      if (player != null && filterPredicateAccessor != null)
        filter = filterPredicateAccessor.accessFilterPredicate(player, _blockStateMeta.getPersistentDataContainer());
    }
  }
}

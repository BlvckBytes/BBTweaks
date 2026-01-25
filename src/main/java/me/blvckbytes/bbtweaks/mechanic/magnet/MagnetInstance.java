package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicInstance;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class MagnetInstance extends SISOInstance implements CuboidMechanicInstance {

  private final Cuboid cuboid;
  private final @Nullable Predicate<ItemStack> filter;

  // It is impossible to cache the inventory-reference over the span of multiple ticks, seeing how
  // there are countless ways for it to get out-of-sync; instead, we cache it for a single tick, which
  // still massively reduces computation as many items can be sucked up all at once.
  private @Nullable Inventory inventory;
  private Material inventoryBlockType;

  private boolean isReferenceUpToDate;
  private boolean didAddItems;
  private boolean wasMissingContainer;

  private boolean enabled;

  public MagnetInstance(Sign sign, Cuboid cuboid, @Nullable Predicate<ItemStack> filter) {
    super(sign);

    this.cuboid = cuboid;
    this.filter = filter;
  }

  private boolean canHold(ItemStack item) {
    if (inventory == null)
      return false;

    // Fast-path: find a vacant slot; well worth iterating twice, instead of needlessly comparing items.
    for (var slot = 0; slot < inventory.getSize(); ++slot) {
      if (inventory.getItem(slot) == null)
        return true;
    }

    var remainingAmount = item.getAmount();

    for (var slot = 0; slot < inventory.getSize(); ++slot) {
      var currentItem = inventory.getItem(slot);

      // Unreachable
      if (currentItem == null)
        continue;

      if (!currentItem.isSimilar(item))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      remainingAmount -= remainingSpace;

      if (remainingAmount <= 0)
        return true;
    }

    return false;
  }

  public boolean acceptsItem(ItemStack item) {
    if (!enabled)
      return false;

    possiblyUpdateInventoryReference();

    if (inventory == null)
      return false;

    if (filter != null && !filter.test(item))
      return false;

    return canHold(item);
  }

  public void addItem(ItemStack item) {
    possiblyUpdateInventoryReference();

    // Unreachable, given that the caller made use of #acceptsItem.
    if (inventory == null)
      return;

    // Remainders are also unreachable, given the above, but just to make absolutely sure.
    inventory.addItem(item)
      .values()
      .forEach(remainder -> mountBlock.getWorld().dropItem(mountBlock.getLocation(), remainder));

    didAddItems = true;
  }

  @Override
  public Cuboid getCuboid() {
    return cuboid;
  }

  @Override
  public boolean tick(int time) {
    isReferenceUpToDate = false;

    if (wasMissingContainer)
      return false;

    var inputPower = tryReadInputPower();
    enabled = inputPower == null || inputPower == 0;

    if (!didAddItems)
      return true;

    didAddItems = false;

    if (inventory instanceof DoubleChestInventory doubleChestInventory) {
      if (doubleChestInventory.getRightSide().getHolder() instanceof Container rightContainer) {
        if (!mountBlock.equals(rightContainer.getBlock())) {
          rightContainer.update(true, true);
          return true;
        }
      }

      if (doubleChestInventory.getLeftSide().getHolder() instanceof Container leftContainer) {
        if (!mountBlock.equals(leftContainer.getBlock()))
          leftContainer.update(true, true);
      }

      return true;
    }

    // For some odd reason, bukkit doesn't cause a block-update for hoppers when modifying
    // their inventory, so we need to cause one manually, as to update comparators and the like.
    if (inventoryBlockType == Material.HOPPER)
      mountBlock.getState().update(true, true);

    return true;
  }

  private void possiblyUpdateInventoryReference() {
    if (isReferenceUpToDate)
      return;

    isReferenceUpToDate = true;

    if (!isBlockLoaded(mountBlock)) {
      inventory = null;
      inventoryBlockType = Material.AIR;
      return;
    }

    if (!(mountBlock.getState() instanceof Container container)) {
      inventory = null;
      inventoryBlockType = Material.AIR;
      wasMissingContainer = true;
      return;
    }

    inventory = container.getInventory();
    inventoryBlockType = container.getType();
  }
}

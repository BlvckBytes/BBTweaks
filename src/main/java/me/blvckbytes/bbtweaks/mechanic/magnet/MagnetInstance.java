package me.blvckbytes.bbtweaks.mechanic.magnet;

import me.blvckbytes.bbtweaks.mechanic.SISOInstance;
import me.blvckbytes.bbtweaks.mechanic.util.Cuboid;
import me.blvckbytes.bbtweaks.mechanic.util.CuboidMechanicInstance;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class MagnetInstance extends SISOInstance implements CuboidMechanicInstance {

  private final Cuboid cuboid;
  private final @Nullable Predicate<ItemStack> filter;

  private @Nullable Inventory inventory;
  private @Nullable Material containerType;
  private boolean enabled;

  public MagnetInstance(
    Block signBlock, BlockFace signFacing,
    Cuboid cuboid, @Nullable Predicate<ItemStack> filter
  ) {
    super(signBlock, signFacing);

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
    if (inventory == null || !enabled)
      return false;

    if (filter != null && !filter.test(item))
      return false;

    return canHold(item);
  }

  public void addItem(ItemStack item) {
    // Unreachable, given that the caller made use of #acceptsItem.
    if (inventory == null)
      return;

    // Remainders are also unreachable, given the above, but just to make absolutely sure.
    inventory.addItem(item)
      .values()
      .forEach(remainder -> mountBlock.getWorld().dropItem(mountBlock.getLocation(), remainder));
  }

  @Override
  public Block getSignBlock() {
    return signBlock;
  }

  @Override
  public Cuboid getCuboid() {
    return cuboid;
  }

  @Override
  public boolean tick(int time) {
    var inputPower = tryReadInputPower();
    enabled = inputPower == null || inputPower == 0;
    return updateInventoryReference();
  }

  private boolean updateInventoryReference() {
    if (!isBlockLoaded(mountBlock)) {
      inventory = null;
      containerType = null;

      // Let's not self-destruct just because the chunk is unloaded;
      // rather wait until we can verify container-presence again.
      return true;
    }

    var mountBlockType = mountBlock.getType();

    if (inventory != null && containerType != null) {
      if (mountBlockType == containerType)
        return true;

      inventory = null;
      containerType = null;
    }

    if (!(mountBlock.getState() instanceof Container container))
      return false;

    inventory = container.getInventory();
    containerType = mountBlockType;

    return true;
  }
}

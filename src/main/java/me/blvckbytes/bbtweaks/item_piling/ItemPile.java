package me.blvckbytes.bbtweaks.item_piling;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import me.blvckbytes.bbtweaks.MainSection;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class ItemPile {

  private static int nextUpdateForcingCustomNameValue = 0;

  private Item itemEntity;
  private final PileEntityMetadataKeeper metadataKeeper;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final NamespacedKey keyAdditionalAmount;

  public ItemPile(
    Item itemEntity,
    PileEntityMetadataKeeper metadataKeeper,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.itemEntity = itemEntity;
    this.metadataKeeper = metadataKeeper;
    this.config = config;
    this.plugin = plugin;

    this.keyAdditionalAmount = new NamespacedKey(plugin, "item-stacking-additional-amount");
  }

  public boolean isWithinDistance(ItemPile other, int distance) {
    var deltaX = itemEntity.getX() - other.itemEntity.getX();
    var deltaY = itemEntity.getY() - other.itemEntity.getY();
    var deltaZ = itemEntity.getZ() - other.itemEntity.getZ();
    var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    return distanceSquared <= distance * distance;
  }

  public Item getItemEntity() {
    return itemEntity;
  }

  public boolean tryAddAndRemoveOther(ItemPile other) {
    if (!getUnitStack().isSimilar(other.getUnitStack()))
      return false;

    addTo(other.getAmountAndType().totalAmount());

    resetTicksLived();

    other.itemEntity.remove();

    return true;
  }

  public ItemStack getUnitStack() {
    var unitStack = new ItemStack(itemEntity.getItemStack());
    unitStack.setAmount(1);
    return unitStack;
  }

  public List<ItemStack> getIndividualStacks() {
    var amountAndType = getAmountAndType();
    var stackSize = amountAndType.type().getMaxStackSize();

    var result = new ArrayList<ItemStack>();
    var remainingAmount = amountAndType.totalAmount();

    while (remainingAmount > 0) {
      var currentAmount = Math.min(remainingAmount, stackSize);
      remainingAmount -= currentAmount;

      var individualStack = getUnitStack();
      individualStack.setAmount(currentAmount);

      result.add(individualStack);
    }

    return result;
  }

  public void reduceBy(int amountToReduceBy, boolean shouldSpawnNew) {
    if (amountToReduceBy <= 0)
      return;

    var additionalAmount = getAdditionalAmount();

    if (additionalAmount > 0) {
      if (additionalAmount >= amountToReduceBy) {
        if (shouldSpawnNew) {
          removeCurrentItemNextTick();
          this.itemEntity = spawnNewItemBasedOnCurrent();
        }

        setAdditionalAmount(additionalAmount - amountToReduceBy);
        updateItemName();
        return;
      }

      amountToReduceBy -= additionalAmount;
      setAdditionalAmount(0);
    }

    var itemStack = itemEntity.getItemStack();

    if (itemStack.getAmount() <= amountToReduceBy) {
      removeCurrentItemNextTick();
      return;
    }

    itemStack.setAmount(itemStack.getAmount() - amountToReduceBy);
    itemEntity.setItemStack(itemStack);

    if (shouldSpawnNew) {
      removeCurrentItemNextTick();
      this.itemEntity = spawnNewItemBasedOnCurrent();
    }

    updateItemName();
  }

  private void resetTicksLived() {
    // We use this value as to allow for rapid stacking when multiple surrounding entities
    // could merge together, as it's low enough that it does not meaningfully decrease the
    // lifespan, but allows us to avoid keeping separate state, to discern ages.
    itemEntity.setTicksLived(config.rootSection.itemPiling.minimumAgeTicks + 1);
  }

  public void addTo(int amountToAdd) {
    if (amountToAdd <= 0)
      return;

    var itemStack = itemEntity.getItemStack();
    var availableSpace = itemStack.getMaxStackSize() - itemStack.getAmount();

    if (availableSpace > 0) {
      var addedAmount = Math.min(availableSpace, amountToAdd);

      itemStack.setAmount(itemStack.getAmount() + addedAmount);
      itemEntity.setItemStack(itemStack);

      amountToAdd -= addedAmount;

      if (amountToAdd == 0) {
        updateItemName();
        return;
      }
    }

    setAdditionalAmount(getAdditionalAmount() + amountToAdd);
    updateItemName();
  }

  private void setAdditionalAmount(int additionalAmount) {
    var pdc = itemEntity.getPersistentDataContainer();

    if (additionalAmount <= 0) {
      pdc.remove(keyAdditionalAmount);
      return;
    }

    pdc.set(keyAdditionalAmount, PersistentDataType.INTEGER, additionalAmount);
  }

  public int getAdditionalAmount() {
    var additionalAmount = itemEntity.getPersistentDataContainer().get(keyAdditionalAmount, PersistentDataType.INTEGER);
    return additionalAmount == null || additionalAmount < 0 ? 0 : additionalAmount;
  }

  public AmountAndType getAmountAndType() {
    var itemStack = itemEntity.getItemStack();
    var totalAmount = itemStack.getAmount() + getAdditionalAmount();
    return new AmountAndType(totalAmount, itemStack.getType());
  }

  public boolean reduceIntoInventoryAndGetIfAny(Inventory inventory, boolean shouldSpawnNew) {
    var totalAddedAmount = 0;

    for (var individualStack : getIndividualStacks()) {
      var availableAmount = individualStack.getAmount();
      var remainingAmount = 0;

      for (var remainingStack : inventory.addItem(individualStack).values())
        remainingAmount += remainingStack.getAmount();

      var addedAmount = availableAmount - remainingAmount;

      totalAddedAmount += addedAmount;

      if (addedAmount < availableAmount)
        break;
    }

    reduceBy(totalAddedAmount, shouldSpawnNew);

    return totalAddedAmount > 0;
  }

  private void removeCurrentItemNextTick() {
    var currentItemEntity = itemEntity;
    currentItemEntity.setPickupDelay(1024);
    Bukkit.getScheduler().runTaskLater(plugin, currentItemEntity::remove, 1L);
  }

  private Item spawnNewItemBasedOnCurrent() {
    var newItem = itemEntity.getWorld().spawn(itemEntity.getLocation(), Item.class);
    newItem.setItemStack(itemEntity.getItemStack());

    newItem.setPickupDelay(0);
    newItem.setVelocity(itemEntity.getVelocity());

    var additionalAmount = getAdditionalAmount();

    if (additionalAmount > 0)
      newItem.getPersistentDataContainer().set(keyAdditionalAmount, PersistentDataType.INTEGER, additionalAmount);

    return newItem;
  }

  public void updateItemName() {
    metadataKeeper.storePileMetadata(itemEntity.getEntityId(), getAmountAndType());
    itemEntity.setCustomNameVisible(true);
    itemEntity.customName(Component.text(String.valueOf(nextUpdateForcingCustomNameValue++)));
  }
}

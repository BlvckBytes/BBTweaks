package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

// TODO: I think that there's an issue with this approach, namely that we do not stack onto
//       similar stacks that have space first, and only then make use of vacant slots; in this
//       case, we may be able to store less than is actually possible, due to a vacant slot possibly
//       having been assigned with a non-maxed-out stack and since we do not look back to ever fill it
//       up again within the same session of adding an item.

// TODO: Maybe we can also use the simulated add only inventory here instead?

public class SpaceSimulator {

  private final int[] slotAmounts;
  private final Material[] slotTypes;

  private boolean didDropItems;

  public SpaceSimulator(Inventory inventory, Function<ItemStack, Material> materialExtractor) {
    var storageContents = inventory.getStorageContents();

    this.slotAmounts = new int[storageContents.length];
    this.slotTypes = new Material[storageContents.length];

    for (var slot = 0; slot < storageContents.length; ++slot) {
      var currentItem = storageContents[slot];

      Material currentMaterial;

      if (currentItem == null || currentItem.getType().isAir()) {
        slotTypes[slot] = Material.AIR;
        continue;
      }

      // Null encodes an incompatible stack (name, lore, enchants, etc.)
      if ((currentMaterial = materialExtractor.apply(currentItem)) == null)
        continue;

      slotTypes[slot] = currentMaterial;
      slotAmounts[slot] = Math.max(0, currentItem.getAmount());
    }
  }

  public void setAmount(int slot, int amount) {
    if ((slotAmounts[slot] = Math.max(0, amount)) == 0)
      slotTypes[slot] = Material.AIR;
  }

  public void takeFromItem(int slot, int amount) {
    if ((slotAmounts[slot] = Math.max(0, slotAmounts[slot] - amount)) == 0)
      slotTypes[slot] = Material.AIR;
  }

  public void addItem(Material material, int amount) {
    for (var slot = 0; slot < slotAmounts.length; ++slot) {
      var slotType = slotTypes[slot];

      // An incompatible item resides at this slot
      if (slotType == null)
        continue;

      var maxTypeAmount = slotType.getMaxStackSize();
      var remainingSpace = maxTypeAmount - slotAmounts[slot];

      if (remainingSpace <= 0)
        continue;

      if (slotType == Material.AIR || slotType == material) {
        slotTypes[slot] = material;
        slotAmounts[slot] += Math.min(amount, remainingSpace);

        if (amount <= remainingSpace)
          return;

        amount -= remainingSpace;
      }
    }

    didDropItems |= amount > 0;
  }

  public boolean didDropItems() {
    return didDropItems;
  }
}

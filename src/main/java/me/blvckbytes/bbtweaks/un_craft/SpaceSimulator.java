package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class SpaceSimulator {

  private final int[] slotAmounts;
  private final Material[] slotTypes;

  private boolean didDropItems;

  public SpaceSimulator(Inventory inventory, Function<ItemStack, @Nullable Material> materialExtractor) {
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

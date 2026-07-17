package me.blvckbytes.bbtweaks.mechanic.auto_crafter;

import me.blvckbytes.bbtweaks.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MatrixCacheHelper {

  private long cachedMatrixMsb;
  private long cachedMatrixLsb;

  public boolean didMatrixChange(ItemStack[] matrixContents) {
    return cachedMatrixMsb != computeMatrixMsb(matrixContents) || cachedMatrixLsb != computeMatrixLsb(matrixContents);
  }

  public void runIfMatrixChanged(ItemStack[] matrixContents, Runnable handler) {
    var priorMatrixMsb = this.cachedMatrixMsb;
    var priorMatrixLsb = this.cachedMatrixLsb;

    this.cachedMatrixMsb = computeMatrixMsb(matrixContents);
    this.cachedMatrixLsb = computeMatrixLsb(matrixContents);

    var isMatrixUnchanged = priorMatrixMsb == cachedMatrixMsb && priorMatrixLsb == cachedMatrixLsb;

    if (isMatrixUnchanged)
      return;

    handler.run();
  }

  private static long computeMatrixMsb(ItemStack[] matrixContents) {
    return (
      getSlotTypeOrdinal(matrixContents, 5)
        | (getSlotTypeOrdinal(matrixContents, 6) << 12)
        | (getSlotTypeOrdinal(matrixContents, 7) << (12 * 2))
        | (getSlotTypeOrdinal(matrixContents, 8) << (12 * 3))
    );
  }

  private static long computeMatrixLsb(ItemStack[] matrixContents) {
    return (
      getSlotTypeOrdinal(matrixContents, 0)
        | (getSlotTypeOrdinal(matrixContents, 1) << 12)
        | (getSlotTypeOrdinal(matrixContents, 2) << (12 * 2))
        | (getSlotTypeOrdinal(matrixContents, 3) << (12 * 3))
        | (getSlotTypeOrdinal(matrixContents, 4) << (12 * 4))
    );
  }

  private static long getSlotTypeOrdinal(ItemStack[] matrixContents, int slot) {
    ItemStack item;

    if (slot < 0 || slot >= matrixContents.length || !ItemUtil.isStackValid(item = matrixContents[slot]))
      return Material.AIR.ordinal();

    return item.getType().ordinal();
  }
}

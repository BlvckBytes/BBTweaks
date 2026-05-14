package me.blvckbytes.bbtweaks.mechanic.common;

import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class TransferCounters {

  public int encounteredContainerItems;
  public boolean encounteredFilterMismatch;

  public final Map<Material, MutableInt> totalTransferredCountByType;
  public final Map<Material, MutableInt> totalExcessCountByType;

  public TransferCounters() {
    this.totalTransferredCountByType = new HashMap<>();
    this.totalExcessCountByType = new HashMap<>();
  }

  public boolean areTypeCountersEmpty() {
    return totalTransferredCountByType.isEmpty() && totalExcessCountByType.isEmpty();
  }
}

package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class UnloadCounters {

  public int encounteredContainerItems;

  public final Map<Material, MutableInt> totalUnloadCountByType;
  public final Map<Material, MutableInt> totalUnfittedCountByType;

  public UnloadCounters() {
    this.totalUnloadCountByType = new HashMap<>();
    this.totalUnfittedCountByType = new HashMap<>();
  }

  public boolean areTypeCountersEmpty() {
    return totalUnloadCountByType.isEmpty() && totalUnfittedCountByType.isEmpty();
  }
}

package me.blvckbytes.bbtweaks.auto_pickup_container;

public record UsageCounts(int usedSlots, int vacantSlots, int containerCount) {

  public static final UsageCounts EMPTY = new UsageCounts(0, 0, 0);

}

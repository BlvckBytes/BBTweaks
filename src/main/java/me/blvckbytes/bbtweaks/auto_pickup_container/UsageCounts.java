package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;

public record UsageCounts(int usedSlots, int vacantSlots, int containerCount) {

  public static final UsageCounts EMPTY = new UsageCounts(0, 0, 0);

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("used_slots", usedSlots)
      .withVariable("vacant_slots", vacantSlots)
      .withVariable("container_count", containerCount);
  }
}

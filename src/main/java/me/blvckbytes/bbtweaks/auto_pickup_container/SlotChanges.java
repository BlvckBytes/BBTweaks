package me.blvckbytes.bbtweaks.auto_pickup_container;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import org.bukkit.entity.Player;

public class SlotChanges {

  private static final long MAX_AGE_T = 2;

  public final Player player;

  private final Int2ObjectMap<SlotChangeData> changeDataBySlot;

  public SlotChanges(Player player) {
    this.player = player;
    this.changeDataBySlot = new Int2ObjectArrayMap<>();
  }

  public void forEachChangedSlotAndUnmark(long relativeTime, IntConsumer slotConsumer) {
    for (var entry : changeDataBySlot.int2ObjectEntrySet()) {
      var changeData = entry.getValue();

      if (!changeData.requiresUpdate)
        continue;

      var changeAge = relativeTime - changeData.lastUpdate;

      if (changeAge <= MAX_AGE_T)
        continue;

      slotConsumer.accept(entry.getIntKey());

      changeData.lastUpdate = relativeTime;
      changeData.requiresUpdate = false;
    }
  }

  public void markRequiringUpdate(int slot) {
    changeDataBySlot
      .computeIfAbsent(slot, k -> new SlotChangeData())
      .requiresUpdate = true;
  }
}

package me.blvckbytes.bbtweaks.auto_pickup_container;

import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.*;

public record MaterialCounts(Map<Material, MutableInt> counts) {

  public static MaterialCounts EMPTY = new MaterialCounts(Collections.emptyMap());

  public List<MaterialCount> asCountList() {
    var result = new ArrayList<MaterialCount>();

    for (var entry : counts.entrySet())
      result.add(new MaterialCount(entry.getKey(), entry.getValue().value));

    return result;
  }

  public static MaterialCounts fromInventory(Inventory inventory) {
    var counts = new EnumMap<Material, MutableInt>(Material.class);
    var inventorySize = inventory.getSize();

    for (var index = 0; index < inventorySize; ++index) {
      var item = inventory.getItem(index);

      if (item == null || item.getType().isAir())
        continue;

      var amount = item.getAmount();

      if (amount <= 0)
        continue;

      counts.computeIfAbsent(item.getType(), k -> new MutableInt()).value += amount;
    }

    return new MaterialCounts(counts);
  }
}

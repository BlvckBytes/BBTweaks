package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PickupTickWindow {
  public final List<ItemBucket> buckets;
  public final Player player;

  public PickupTickWindow(Player player) {
    this.buckets = new ArrayList<>();
    this.player = player;
  }

  public ItemBucket accessOrCreateBucket(ItemStack item) {
    for (var existingBucket : buckets) {
      if (existingBucket.item.isSimilar(item))
        return existingBucket;
    }

    var newBucket = new ItemBucket(item);

    buckets.add(newBucket);

    return newBucket;
  }
}

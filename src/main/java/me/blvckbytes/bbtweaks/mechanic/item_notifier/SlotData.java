package me.blvckbytes.bbtweaks.mechanic.item_notifier;

import org.bukkit.Material;

public record SlotData(Material itemType, int amount) {

  public static final SlotData EMPTY = new SlotData(Material.AIR, 0);

}

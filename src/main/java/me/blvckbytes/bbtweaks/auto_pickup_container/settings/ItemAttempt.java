package me.blvckbytes.bbtweaks.auto_pickup_container.settings;

import org.bukkit.inventory.ItemStack;

public record ItemAttempt(
  ItemStack item,
  long relativeTime
) {}

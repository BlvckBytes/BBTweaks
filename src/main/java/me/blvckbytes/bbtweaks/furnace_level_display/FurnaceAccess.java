package me.blvckbytes.bbtweaks.furnace_level_display;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.bukkit.inventory.FurnaceInventory;

public record FurnaceAccess(
  Reference2IntMap<?> recipesUsed,
  FurnaceInventory inventory
) {}

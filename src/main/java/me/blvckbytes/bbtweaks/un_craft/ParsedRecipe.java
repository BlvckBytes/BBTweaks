package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

public record ParsedRecipe(
  Material uncraftedItemType,
  int uncraftedItemAmount,
  Map<Material, Integer> uncraftResults,
  Set<Material> subtractedResults
) {}

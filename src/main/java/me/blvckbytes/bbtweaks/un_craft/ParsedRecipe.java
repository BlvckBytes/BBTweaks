package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;

import java.util.Map;

public record ParsedRecipe(
  Material uncraftedItemType,
  int uncraftedItemAmount,
  Map<Material, Integer> uncraftResults
) {}

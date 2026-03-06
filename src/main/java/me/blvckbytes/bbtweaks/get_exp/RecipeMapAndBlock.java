package me.blvckbytes.bbtweaks.get_exp;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import org.bukkit.block.Block;

public record RecipeMapAndBlock(Reference2IntMap<?> recipeMap, Block block) {}

package me.blvckbytes.bbtweaks.furnace_level_display;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface RecipesUsedAccessor {

  @Nullable Reference2IntOpenHashMap<?> access(BlockState blockState);

}

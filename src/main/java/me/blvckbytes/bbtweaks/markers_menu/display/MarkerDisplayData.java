package me.blvckbytes.bbtweaks.markers_menu.display;

import me.blvckbytes.bbtweaks.markers_menu.CategorySection;
import org.jetbrains.annotations.Nullable;

public record MarkerDisplayData(
  @Nullable CategorySection selectedCategory,
  @Nullable MarkerDisplay previousDisplay
) {}

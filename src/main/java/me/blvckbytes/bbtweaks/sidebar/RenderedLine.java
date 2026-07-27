package me.blvckbytes.bbtweaks.sidebar;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public record RenderedLine(
  Component component,
  @Nullable SidebarStatistic statistic,
  int sortingValue
) {}

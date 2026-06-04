package me.blvckbytes.bbtweaks.sidebar.color_display;

import me.blvckbytes.bbtweaks.sidebar.config.StatisticSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import org.jetbrains.annotations.Nullable;

public record ColorDisplayData(
  SidebarPreferences preferences,
  @Nullable StatisticSection statistic,
  Runnable backHandler
) {}
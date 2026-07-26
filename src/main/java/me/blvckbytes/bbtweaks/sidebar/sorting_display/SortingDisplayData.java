package me.blvckbytes.bbtweaks.sidebar.sorting_display;

import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;

public record SortingDisplayData(
  SidebarPreferences preferences,
  int initialPage,
  Runnable backHandler
) {}

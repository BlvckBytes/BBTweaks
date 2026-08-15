package me.blvckbytes.bbtweaks.integration.placeholder_api;

import org.jetbrains.annotations.Nullable;

public class ComputedValueCache {

  private static final long VALUE_CACHE_MAX_AGE_T = 10;

  private long lastUpdate;

  public @Nullable String donorSymbolPrependedLuckPermsSuffixForJava;
  public @Nullable String donorSymbolPrependedLuckPermsSuffixForBedrock;

  public ComputedValueCache() {
    this.lastUpdate = -1;
  }

  public boolean touchLastUpdateIfApplicable(long relativeTime) {
    if (relativeTime >= 0 && relativeTime - lastUpdate <= VALUE_CACHE_MAX_AGE_T)
      return false;

    lastUpdate = relativeTime;
    return true;
  }
}

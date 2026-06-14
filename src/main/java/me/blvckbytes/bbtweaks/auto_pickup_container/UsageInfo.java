package me.blvckbytes.bbtweaks.auto_pickup_container;

import org.bukkit.entity.Player;

public class UsageInfo {

  public final Player player;

  public UsageCounts lastKnownCounts;
  public long lastUpdateTime;
  public boolean possiblyChanged;

  public UsageInfo(Player player, UsageCounts lastKnownCounts, long relativeTime) {
    this.player = player;

    this.lastKnownCounts = lastKnownCounts;
    this.lastUpdateTime = relativeTime;
  }
}

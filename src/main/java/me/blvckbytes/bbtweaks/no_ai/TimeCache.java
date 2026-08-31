package me.blvckbytes.bbtweaks.no_ai;

import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.UUID;

public class TimeCache {

  private final Object2LongMap<UUID> worldTimeByWorldId;
  private final Object2BooleanMap<UUID> isNonNormalWorldByWorldId;

  private TimeCache() {
    this.worldTimeByWorldId = new Object2LongArrayMap<>();
    this.worldTimeByWorldId.defaultReturnValue(-1);

    this.isNonNormalWorldByWorldId = new Object2BooleanArrayMap<>();
    this.isNonNormalWorldByWorldId.defaultReturnValue(false);
  }

  public boolean isNonNormalWorldOrDayTime(World world) {
    var worldId = world.getUID();

    if (isNonNormalWorldByWorldId.getBoolean(worldId))
      return true;

    var worldTime = worldTimeByWorldId.getLong(worldId);

    if (worldTime < 0)
      return false;

    var relativeTime = worldTime % 24_000;

    return relativeTime <= 12_500;
  }

  public long getFullTime(World world) {
    return worldTimeByWorldId.getLong(world.getUID());
  }

  public static TimeCache captureCurrentTimes() {
    var result = new TimeCache();

    for (var world : Bukkit.getWorlds()) {
      var worldId = world.getUID();

      result.worldTimeByWorldId.put(worldId, world.getFullTime());
      result.isNonNormalWorldByWorldId.put(worldId, world.getEnvironment() != World.Environment.NORMAL);
    }

    return result;
  }
}

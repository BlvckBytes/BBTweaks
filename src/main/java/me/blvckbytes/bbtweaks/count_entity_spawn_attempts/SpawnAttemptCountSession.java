package me.blvckbytes.bbtweaks.count_entity_spawn_attempts;

import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;

public class SpawnAttemptCountSession {

  public final long startStamp;
  public final World world;

  public final EnumMap<EntityType, MutableInt> counts;

  public SpawnAttemptCountSession(long startStamp, World world) {
    this.startStamp = startStamp;
    this.world = world;

    this.counts = new EnumMap<>(EntityType.class);
  }
}

package me.blvckbytes.bbtweaks.no_ai.simulator;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.AfterStartup;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.no_ai.TimeCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class BehaviorSimulator<MobType extends Mob> implements Listener, AfterStartup, Disableable {

  protected final Plugin plugin;
  protected final ConfigKeeper<MainSection> config;
  public final Class<MobType> mobType;
  public final EntityType entityType;
  protected final List<MobType> simulatedMobs;

  private final NamespacedKey keyBehaviorSimulated, keyLastMakeLook;

  protected BehaviorSimulator(
    Plugin plugin,
    ConfigKeeper<MainSection> config,
    Class<MobType> mobType,
    EntityType entityType
  ) {
    this.plugin = plugin;
    this.config = config;
    this.mobType = mobType;
    this.entityType = entityType;
    this.simulatedMobs = new ArrayList<>();

    this.keyBehaviorSimulated = new NamespacedKey(plugin, "behavior-simulated");
    this.keyLastMakeLook = new NamespacedKey(plugin, "last-make-look");
  }

  public abstract void sendStatusMessage(Player player, Entity entity);

  protected abstract void simulate(MobType mob, TimeCache timeCache);

  @Override
  public void disable() {
    for (var entity : new ArrayList<>(simulatedMobs))
      removeSimulated(entity, false);
  }

  @Override
  public void afterStartup() {
    for (var world : Bukkit.getWorlds()) {
      for (var entity : world.getEntitiesByClass(mobType))
        addSimulated(entity, false);
    }
  }

  @EventHandler
  public void onEntityAddToWorld(EntityAddToWorldEvent event) {
    var entity = event.getEntity();

    if (isHandledInstance(entity))
      addSimulated(entity, false);
  }

  @EventHandler
  public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
    var entity = event.getEntity();

    if (isHandledInstance(entity))
      removeSimulated(entity, false);
  }

  public void makeLook(Player player, Entity entity) {
    ensureIsInstance(entity);

    var mob = mobType.cast(entity);

    var lookLocation = mob.getEyeLocation();
    lookLocation.setDirection(player.getEyeLocation().toVector().subtract(lookLocation.toVector()));
    entity.setRotation(lookLocation.getYaw(), lookLocation.getPitch());

    writeLastMakeLook(entity, lookLocation);
  }

  private void writeLastMakeLook(Entity entity, Location location) {
    var buffer = ByteBuffer.allocate(2 * Float.BYTES);

    buffer.putFloat(location.getYaw());
    buffer.putFloat(location.getPitch());

    entity.getPersistentDataContainer().set(keyLastMakeLook, PersistentDataType.BYTE_ARRAY, buffer.array());
  }

  private void applyLastLookNextTickIfSet(Entity entity) {
    var data = entity.getPersistentDataContainer().get(keyLastMakeLook, PersistentDataType.BYTE_ARRAY);

    if (data == null)
      return;

    var buffer = ByteBuffer.wrap(data);

    float yaw, pitch;

    try {
      yaw = buffer.getFloat();
      pitch = buffer.getFloat();
    } catch (Throwable e) {
      return;
    }

    Bukkit.getScheduler().runTaskLater(plugin, () -> entity.setRotation(yaw, pitch), 1L);
  }

  public boolean isHandledInstance(Entity entity) {
    return mobType.isInstance(entity);
  }

  public boolean isEntityIdInSimulatedList(Entity entity) {
    var entityId = entity.getEntityId();

    for (var simulatedMob : simulatedMobs) {
      if (simulatedMob.getEntityId() == entityId)
        return true;
    }

    return false;
  }

  public void addSimulated(Entity entity, boolean markIfUnmarked) {
    ensureIsInstance(entity);

    var pdc = entity.getPersistentDataContainer();
    var markerValue = pdc.get(keyBehaviorSimulated, PersistentDataType.BOOLEAN);

    if (markerValue == null) {
      if (!markIfUnmarked)
        return;

      pdc.set(keyBehaviorSimulated, PersistentDataType.BOOLEAN, true);
    }

    if (isEntityIdInSimulatedList(entity))
      return;

    var mob = mobType.cast(entity);

    mob.setAware(false);

    applyLastLookNextTickIfSet(entity);

    simulatedMobs.add(mob);
  }

  public boolean removeSimulated(Entity entity, boolean unmarkIfMarked) {
    ensureIsInstance(entity);

    if (unmarkIfMarked)
      entity.getPersistentDataContainer().remove(keyBehaviorSimulated);

    var entityId = entity.getEntityId();

    for (var index = simulatedMobs.size() - 1; index >= 0; --index) {
      var simulatedMob = simulatedMobs.get(index);

      if (simulatedMob.getEntityId() != entityId)
        continue;

      simulatedMob.setAware(true);

      simulatedMobs.remove(index);
      return true;
    }

    return false;
  }

  public boolean toggleAndGetIfNowPresent(Entity entity) {
    if (removeSimulated(entity, true))
      return false;

    addSimulated(entity, true);
    return true;
  }

  public void tick(TimeCache timeCache) {
    for (var mob : simulatedMobs) {
      try {
        simulate(mob, timeCache);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "An error occurred while processing a simulated entity at " + mob.getLocation(), e);
      }
    }
  }

  public void ensureIsInstance(Entity entity) {
    if (!isHandledInstance(entity))
      throw new IllegalArgumentException("Expected " + mobType + " but got " + (entity == null ? null : entity.getClass()));
  }
}

package me.blvckbytes.bbtweaks.worldguard_flags;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.RegistryFlag;
import com.sk89q.worldguard.protection.flags.SetFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.LateWired;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.*;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WorldGuardFlags implements Listener, Tickable {

  private static final Set<DamageType> HURT_BY_HEAT_DAMAGE_TYPES = Set.of(
    DamageType.HOT_FLOOR, DamageType.IN_FIRE, DamageType.ON_FIRE, DamageType.CAMPFIRE, DamageType.LAVA
  );

  private static final long UNUSED_VEHICLES_REMOVAL_PERIOD_T = 10;

  private final StateFlag lecternTakeFlag;
  private final StateFlag lecternInsertFlag;
  private final StateFlag elytraBoostFlag;
  private final StateFlag spawnerChangeFlag;
  private final StateFlag hurtByHeatFlag;
  private final StateFlag chiseledBookshelfInteractFlag;
  private final StateFlag shelfInteractFlag;
  private final StateFlag mobSpawningFlag;
  private final StateFlag naturalSpawning;
  private final StateFlag vehicleCollide;
  private final StateFlag decoratedPotPut;

  private final SetFlag<EntityType> allowSpawnFlag;
  private final SetFlag<EntityType> denySpawnFlag;
  private final SetFlag<EntityType> removeUnusedVehicles;

  private final NamespacedKey keyLastVehicleUse;

  // Used to keep track of events in the priority call-sequence. If we cancel an event
  // before WorldGuard encounters it, we can bypass its calculations and thereby
  // selectively allow certain interactions based on additional flags of a region.
  private final Set<PlayerInteractEvent> temporarilyCancelledInteractEvents;

  private final Plugin plugin;

  @LateWired
  private ConfigKeeper<MainSection> config;

  private final Map<UUID, BukkitTask> lastClearFireTaskByPlayerId;

  public WorldGuardFlags(Plugin plugin) {
    var flagRegistry = WorldGuard.getInstance().getFlagRegistry();

    lecternTakeFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("lectern-take", true));
    lecternInsertFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("lectern-insert", true));
    elytraBoostFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("elytra-boost", true));
    spawnerChangeFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("spawner-change", true));
    hurtByHeatFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("hurt-by-heat", true));
    chiseledBookshelfInteractFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("chiseled-bookshelf-interact", true));
    shelfInteractFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("shelf-interact", true));
    naturalSpawning = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("natural-spawning", true));
    vehicleCollide = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("vehicle-collide", true));
    decoratedPotPut = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("decorated-pot-put", false));

    if (!(flagRegistry.get("mob-spawning") instanceof StateFlag _mobSpawningFlag))
      throw new IllegalStateException("Expected the WG-flag \"mob-spawning\" to be a registered StateFlag");

    mobSpawningFlag = _mobSpawningFlag;

    allowSpawnFlag = tryRegisterSetFlagOrFail(flagRegistry, new SetFlag<>("allow-spawn", new RegistryFlag<>(null, EntityType.REGISTRY)));

    if (!(flagRegistry.get("deny-spawn") instanceof SetFlag<?> _denySpawnFlag))
      throw new IllegalStateException("Expected the WG-flag \"deny-spawn\" to be a registered SetFlag");

    //noinspection unchecked
    denySpawnFlag = (SetFlag<EntityType>) _denySpawnFlag;

    removeUnusedVehicles = tryRegisterSetFlagOrFail(flagRegistry, new SetFlag<>("remove-unused-vehicles", new RegistryFlag<>(null, EntityType.REGISTRY)));

    keyLastVehicleUse = new NamespacedKey(plugin, "last-vehicle-use");

    this.temporarilyCancelledInteractEvents = new HashSet<>();

    this.plugin = plugin;

    this.lastClearFireTaskByPlayerId = new HashMap<>();
  }

  private static <T> SetFlag<T> tryRegisterSetFlagOrFail(FlagRegistry flagRegistry, SetFlag<T> flag) {
    try {
      flagRegistry.register(flag);
      return flag;
    } catch (FlagConflictException e) {
      throw new IllegalStateException("The WG-flag \"" + flag.getName() + "\" was already taken");
    }
  }

  private static StateFlag tryRegisterStateFlagOrFail(FlagRegistry flagRegistry, StateFlag flag) {
    try {
      flagRegistry.register(flag);
      return flag;
    } catch (FlagConflictException e) {
      var existing = flagRegistry.get(flag.getName());

      if (!(existing instanceof StateFlag stateFlag))
        throw new IllegalStateException("The WG-flag \"" + flag.getName() + "\" was already taken as a non-state-flag");

      return stateFlag;
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onLecternTake(PlayerTakeLecternBookEvent event) {
    if (isFlagDeniedForAt(event.getPlayer(), event.getLectern().getLocation(), lecternTakeFlag))
      event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onLecternInsert(PlayerInsertLecternBookEvent event) {
    if (isFlagDeniedForAt(event.getPlayer(), event.getLectern().getLocation(), lecternInsertFlag))
      event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onElytraBoost(PlayerElytraBoostEvent event) {
    if (isFlagDeniedForAt(event.getPlayer(), event.getPlayer().getLocation(), elytraBoostFlag))
      event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onVehicleEntityCollision(VehicleEntityCollisionEvent event) {
    if (isFlagDeniedForAt(null, event.getVehicle().getLocation(), vehicleCollide))
      event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPreWorldGuardInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    // A plugin other than WorldGuard denied interaction - do not override later on.
    if (event.useInteractedBlock() == Event.Result.DENY)
      return;

    var player = event.getPlayer();
    var blockType = clickedBlock.getType();

    if (blockType == Material.DECORATED_POT) {
      // Temporarily cancel at the early stage before WorldGuard sees it as to avoid
      // the chat-message telling the player that they cannot do that here.
      if (isFlagAllowedAt(player, clickedBlock.getLocation(), decoratedPotPut)) {
        temporarilyCancelledInteractEvents.add(event);
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPostWorldGuardInteract(PlayerInteractEvent event) {
    // Always remove, ahead of all else, as to absolutely avoid leaking memory. Cheap enough.
    var wasTemporarilyCancelled = temporarilyCancelledInteractEvents.remove(event);

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    var player = event.getPlayer();
    var blockType = clickedBlock.getType();

    if (blockType == Material.DECORATED_POT) {
      if (wasTemporarilyCancelled) {
        if (event.useInteractedBlock() == Event.Result.DENY) {
          if (isFlagAllowedAt(player, clickedBlock.getLocation(), decoratedPotPut))
            event.setCancelled(false);

          return;
        }

        return;
      }

      if (isFlagDeniedForAt(player, clickedBlock.getLocation(), decoratedPotPut))
        event.setCancelled(true);

      return;
    }

    if (blockType == Material.SPAWNER) {
      var heldItem = event.getItem();

      if (heldItem == null || !heldItem.getType().getKey().getKey().endsWith("_spawn_egg"))
        return;

      if (isFlagDeniedForAt(player, clickedBlock.getLocation(), spawnerChangeFlag))
        event.setCancelled(true);

      return;
    }

    if (blockType == Material.CHISELED_BOOKSHELF) {
      if (isFlagDeniedForAt(player, clickedBlock.getLocation(), chiseledBookshelfInteractFlag))
        event.setCancelled(true);

      return;
    }

    if (Tag.WOODEN_SHELVES.isTagged(blockType)) {
      if (isFlagDeniedForAt(player, clickedBlock.getLocation(), shelfInteractFlag))
        event.setCancelled(true);
    }
  }

  private boolean isFlagAllowedAt(Player player, Location location, StateFlag stateFlag) {
    var query = WorldGuard.getInstance()
      .getPlatform()
      .getRegionContainer()
      .createQuery();

    var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
    var wgLocation = BukkitAdapter.adapt(location);

    return query.testBuild(wgLocation, wgPlayer, stateFlag);
  }

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var damageType = event.getDamageSource().getDamageType();

    if (!HURT_BY_HEAT_DAMAGE_TYPES.contains(damageType))
      return;

    if (!isFlagDeniedForAt(player, player.getLocation(), hurtByHeatFlag, FlagTestOption.NO_BYPASS_CHECK))
      return;

    event.setCancelled(true);

    var clearFireTask = Bukkit.getScheduler().runTaskLater(plugin, () -> player.setFireTicks(0), 5L);
    var previousTask = lastClearFireTaskByPlayerId.put(player.getUniqueId(), clearFireTask);

    if (previousTask != null)
      previousTask.cancel();
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    var reason = event.getSpawnReason();

    if (reason == CreatureSpawnEvent.SpawnReason.NATURAL) {
      if (isFlagDeniedForAt(null, event.getLocation(), naturalSpawning, FlagTestOption.DENIED_ON_ALL_REGIONS)) {
        event.setCancelled(true);
        return;
      }
    }

    if (reason != CreatureSpawnEvent.SpawnReason.COMMAND && reason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
      return;

    var allowedEntities = querySetFlagValueAt(event.getLocation(), allowSpawnFlag);

    if (allowedEntities == null)
      return;

    var weEntityType = BukkitAdapter.adapt(event.getEntityType());

    if (!allowedEntities.contains(weEntityType)) {
      event.setCancelled(true);
      return;
    }

    if (!event.isCancelled())
      return;

    // Allow to override a denial with an allowance - especially useful with overlapping regions.

    var deniedEntities = querySetFlagValueAt(event.getLocation(), denySpawnFlag);

    if (deniedEntities != null && deniedEntities.contains(weEntityType)) {
      event.setCancelled(false);
      return;
    }

    if (isFlagDeniedForAt(null, event.getLocation(), mobSpawningFlag))
      event.setCancelled(false);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    lastClearFireTaskByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private <T> @Nullable Set<T> querySetFlagValueAt(Location location, SetFlag<T> flag) {
    var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var query = container.createQuery();

    var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));

    return regions.queryValue(null, flag);
  }

  private boolean isFlagDeniedForAt(@Nullable Player player, Location location, StateFlag flag, FlagTestOption... options) {
    if (options.length == 0)
      return isFlagDeniedForAt(player, location, flag, EnumSet.noneOf(FlagTestOption.class));

    return isFlagDeniedForAt(player, location, flag, EnumSet.of(options[0], options));
  }

  private boolean isFlagDeniedForAt(@Nullable Player player, Location location, StateFlag flag, EnumSet<FlagTestOption> options) {
    var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var query = container.createQuery();

    var regionSet = query.getApplicableRegions(BukkitAdapter.adapt(location));
    var wgPlayer = player == null ? null : WorldGuardPlugin.inst().wrapPlayer(player);

    var state = regionSet.queryState(wgPlayer, flag);

    if (state != StateFlag.State.DENY)
      return false;

    var world = location.getWorld();

    if (world != null && player != null && !options.contains(FlagTestOption.NO_BYPASS_CHECK)) {
      if (player.hasPermission("worldguard.bypass." + world.getName()))
        return false;
    }

    if (options.contains(FlagTestOption.DENIED_ON_ALL_REGIONS)) {
      for (var applicableRegion : regionSet) {
        if (applicableRegion.getFlag(flag) != StateFlag.State.DENY)
          return false;
      }
    }

    return true;
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % 5 == 0)
      handleHurtByHeatFireResistance();

    if (relativeTime % UNUSED_VEHICLES_REMOVAL_PERIOD_T == 0)
      scanForUnusedVehicles();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onVehicleEnter(VehicleEnterEvent event) {
    touchLastVehicleUseValue(event.getVehicle().getPersistentDataContainer());
  }

  private void scanForUnusedVehicles() {
    if (config.rootSection.worldGuardFlags.unusedVehicleDurationSeconds <= 0)
      return;

    for (var world : Bukkit.getWorlds()) {
      for (var vehicle : world.getEntitiesByClass(Vehicle.class))
        removeUnusedVehicleIfApplicable(vehicle);
    }
  }

  // Allows for clearer vision while swimming under lava.
  private void handleHurtByHeatFireResistance() {
    for (var player : Bukkit.getOnlinePlayers()) {
      if (!player.isInLava())
        continue;

      var fireResistance = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);

      if (fireResistance != null && fireResistance.getDuration() > 20)
        continue;

      if (!isFlagDeniedForAt(player, player.getLocation(), hurtByHeatFlag, FlagTestOption.NO_BYPASS_CHECK))
        continue;

      player.addPotionEffect(new PotionEffect(
        PotionEffectType.FIRE_RESISTANCE,
        20 * 5, 0,
        false, false, false
      ));
    }
  }

  private void removeUnusedVehicleIfApplicable(Vehicle vehicle) {
    var vehiclePdc = vehicle.getPersistentDataContainer();

    if (!vehicle.getPassengers().isEmpty()) {
      touchLastVehicleUseValue(vehiclePdc);
      return;
    }

    var removedEntities = querySetFlagValueAt(vehicle.getLocation(), removeUnusedVehicles);

    if (removedEntities == null)
      return;

    var weEntityType = BukkitAdapter.adapt(vehicle.getType());

    if (!removedEntities.contains(weEntityType))
      return;

    var lastUseValue = vehiclePdc.get(keyLastVehicleUse, PersistentDataType.LONG);

    if (lastUseValue == null) {
      touchLastVehicleUseValue(vehiclePdc);
      return;
    }

    var unusedDurationSeconds = (System.currentTimeMillis() - lastUseValue) / 1000;

    if (unusedDurationSeconds < config.rootSection.worldGuardFlags.unusedVehicleDurationSeconds)
      return;

    vehicle.remove();
  }

  private void touchLastVehicleUseValue(PersistentDataContainer pdc) {
    pdc.set(keyLastVehicleUse, PersistentDataType.LONG, System.currentTimeMillis());
  }
}

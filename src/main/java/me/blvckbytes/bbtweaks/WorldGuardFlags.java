package me.blvckbytes.bbtweaks;

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
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class WorldGuardFlags implements Listener, Tickable {

  private static final Set<DamageType> HURT_BY_HEAT_DAMAGE_TYPES = Set.of(
    DamageType.HOT_FLOOR, DamageType.IN_FIRE, DamageType.ON_FIRE, DamageType.CAMPFIRE, DamageType.LAVA
  );

  private final StateFlag lecternTakeFlag;
  private final StateFlag lecternInsertFlag;
  private final StateFlag elytraBoostFlag;
  private final StateFlag spawnerChangeFlag;
  private final StateFlag hurtByHeatFlag;
  private final StateFlag chiseledBookshelfInteractFlag;
  private final StateFlag shelfInteractFlag;
  private final StateFlag mobSpawningFlag;

  private final SetFlag<EntityType> allowSpawnFlag;
  private final SetFlag<EntityType> denySpawnFlag;

  private final Plugin plugin;

  public WorldGuardFlags(Plugin plugin) {
    var flagRegistry = WorldGuard.getInstance().getFlagRegistry();

    lecternTakeFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("lectern-take", true));
    lecternInsertFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("lectern-insert", true));
    elytraBoostFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("elytra-boost", true));
    spawnerChangeFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("spawner-change", true));
    hurtByHeatFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("hurt-by-heat", true));
    chiseledBookshelfInteractFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("chiseled-bookshelf-interact", true));
    shelfInteractFlag = tryRegisterStateFlagOrFail(flagRegistry, new StateFlag("shelf-interact", true));

    if (!(flagRegistry.get("mob-spawning") instanceof StateFlag _mobSpawningFlag))
      throw new IllegalStateException("Expected the WG-flag \"mob-spawning\" to be a registered StateFlag");

    mobSpawningFlag = _mobSpawningFlag;

    allowSpawnFlag = tryRegisterSetFlagOrFail(flagRegistry, new SetFlag<>("allow-spawn", new RegistryFlag<>(null, EntityType.REGISTRY)));

    if (!(flagRegistry.get("deny-spawn") instanceof SetFlag<?> _denySpawnFlag))
      throw new IllegalStateException("Expected the WG-flag \"deny-spawn\" to be a registered SetFlag");

    //noinspection unchecked
    denySpawnFlag = (SetFlag<EntityType>) _denySpawnFlag;

    this.plugin = plugin;
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
  public void onInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    var player = event.getPlayer();
    var blockType = clickedBlock.getType();

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

  @EventHandler(ignoreCancelled = true)
  public void onDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player))
      return;

    var damageType = event.getDamageSource().getDamageType();

    if (!HURT_BY_HEAT_DAMAGE_TYPES.contains(damageType))
      return;

    if (!isFlagDeniedForAt(player, player.getLocation(), hurtByHeatFlag))
      return;

    event.setCancelled(true);

    // Also clear fire-ticks immediately after leaving the lava/fire/etc.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      player.setFireTicks(0);
    }, 1L);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    var reason = event.getSpawnReason();

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

  private <T> @Nullable Set<T> querySetFlagValueAt(Location location, SetFlag<T> flag) {
    var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var query = container.createQuery();

    var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));

    return regions.queryValue(null, flag);
  }

  private boolean isFlagDeniedForAt(@Nullable Player player, Location location, StateFlag flag) {
    var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var query = container.createQuery();

    var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
    var wgPlayer = player == null ? null : WorldGuardPlugin.inst().wrapPlayer(player);

    var state = regions.queryState(wgPlayer, flag);

    if (state == StateFlag.State.DENY) {
      var world = location.getWorld();

      if (world == null || player == null)
        return true;

      return !player.hasPermission("worldguard.bypass." + world.getName());
    }

    return false;
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % 5 == 0)
      handleHurtByHeatFireResistance();
  }

  // Allows for clearer vision while swimming under lava.
  private void handleHurtByHeatFireResistance() {
    for (var player : Bukkit.getOnlinePlayers()) {
      if (!player.isInLava())
        continue;

      var fireResistance = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);

      if (fireResistance != null && fireResistance.getDuration() > 20)
        continue;

      if (!isFlagDeniedForAt(player, player.getLocation(), hurtByHeatFlag))
        continue;

      player.addPotionEffect(new PotionEffect(
        PotionEffectType.FIRE_RESISTANCE,
        20 * 5, 0,
        false, false, false
      ));
    }
  }
}

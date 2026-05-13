package me.blvckbytes.bbtweaks;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

public class WorldGuardFlags implements Listener {

  private final StateFlag lecternTakeFlag;
  private final StateFlag lecternInsertFlag;
  private final StateFlag elytraBoostFlag;
  private final StateFlag spawnerChangeFlag;

  public WorldGuardFlags() {
    lecternTakeFlag = tryRegisterFlagOrFail("lectern-take");
    lecternInsertFlag = tryRegisterFlagOrFail("lectern-insert");
    elytraBoostFlag = tryRegisterFlagOrFail("elytra-boost");
    spawnerChangeFlag = tryRegisterFlagOrFail("spawner-change");
  }

  private StateFlag tryRegisterFlagOrFail(String name) {
    var registry = WorldGuard.getInstance().getFlagRegistry();

    try {
      var flag = new StateFlag(name, true);
      registry.register(flag);
      return flag;
    } catch (FlagConflictException e) {
      var existing = registry.get(name);

      if (!(existing instanceof StateFlag stateFlag))
        throw new IllegalStateException("The WG-flag \"" + name + "\" was already taken as a non-state-flag");

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

    if (clickedBlock == null || clickedBlock.getType() != Material.SPAWNER)
      return;

    var heldItem = event.getItem();

    if (heldItem == null || !heldItem.getType().getKey().getKey().endsWith("_spawn_egg"))
      return;

    if (isFlagDeniedForAt(event.getPlayer(), clickedBlock.getLocation(), spawnerChangeFlag))
      event.setCancelled(true);
  }

  private boolean isFlagDeniedForAt(Player player, Location location, StateFlag flag) {
    var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    var query = container.createQuery();

    var regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
    var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

    var state = regions.queryState(wgPlayer, flag);

    if (state == StateFlag.State.DENY) {
      var world = location.getWorld();
      return world == null || !player.hasPermission("worldguard.bypass." + world.getName());
    }

    return false;
  }
}

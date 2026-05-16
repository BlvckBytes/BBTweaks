package me.blvckbytes.bbtweaks.inv_magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class InvMagnetCommand implements CommandExecutor, TabCompleter, Listener {

  private final InvMagnetParametersStore parametersStore;
  private final ConfigKeeper<MainSection> config;
  private final Logger logger;

  private final Int2ObjectMap<EntityAttractionSession> perTickAttractionSessionByEntityId;
  private final List<World> worlds;

  public InvMagnetCommand(
    Plugin plugin,
    InvMagnetParametersStore parametersStore,
    ConfigKeeper<MainSection> config
  ) {
    this.parametersStore = parametersStore;
    this.config = config;
    this.logger = plugin.getLogger();

    this.perTickAttractionSessionByEntityId = new Int2ObjectArrayMap<>();
    this.worlds = new ArrayList<>();

    loadWorldsFromConfig();

    config.registerReloadListener(this::loadWorldsFromConfig);

    Bukkit.getScheduler().runTaskTimer(plugin, this::attractNearbyItemsAndOrbs, 0L, 1L);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!player.hasPermission("bbtweaks.invmagnet")) {
      config.rootSection.invMagnet.missingPermission.sendMessage(player);
      return true;
    }

    if (!worlds.contains(player.getWorld())) {
      config.rootSection.invMagnet.unallowedWorld.sendMessage(player);
      return true;
    }

    var parameter = parametersStore.accessParameters(player);

    parameter.updateLimitsAndConstrain();

    if (args.length == 0) {
      parameter.enabled ^= true;

      if (parameter.enabled) {
        config.rootSection.invMagnet.nowEnabled.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("radius", parameter.getRadius())
        );

        return true;
      }

      config.rootSection.invMagnet.nowDisabled.sendMessage(player);
      return true;
    }

    var newRadius = -1;

    try {
      newRadius = Integer.parseInt(args[0]);
    } catch (Throwable ignored) {}

    if (newRadius < 0) {
      config.rootSection.invMagnet.invalidRadius.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", args[0])
      );

      return true;
    }

    if (parameter.setRadiusAndGetIfExceeded(newRadius)) {
      config.rootSection.invMagnet.exceededRadiusLimit.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("radius", newRadius)
          .withVariable("radius_limit", parameter.getLimits().maxRadius())
      );
    }

    config.rootSection.invMagnet.updatedRadius.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("radius", parameter.getRadius())
    );

    if (!parameter.enabled)
      parameter.enabled = true;

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !sender.hasPermission("bbtweaks.invmagnet"))
      return List.of();

    if (args.length == 1) {
      var parameter = parametersStore.accessParameters(player);

      parameter.updateLimitsAndConstrain();

      var radii = new ArrayList<String>();

      for (var radius = 1; radius <= parameter.getLimits().maxRadius(); ++radius)
        radii.add(String.valueOf(radius));

      return radii;
    }

    return List.of();
  }

  private void attractNearbyItemsAndOrbs() {
    for (var world : worlds) {
      perTickAttractionSessionByEntityId.clear();

      for (var player : world.getPlayers()) {
        if (player.getGameMode() != GameMode.SURVIVAL)
          continue;

        var parameter = parametersStore.accessParameters(player);
        var radius = parameter.getRadius();

        if (!parameter.enabled || radius <= 0)
          continue;

        // Attract near their chest
        var playerLocation = player.getLocation().add(0, .75, 0);

        for (var nearbyEntity : player.getNearbyEntities(radius, radius, radius)) {
          if (nearbyEntity.isDead() || !nearbyEntity.isValid())
            continue;

          if (nearbyEntity instanceof Item item) {
            if (item.getPickupDelay() > 0)
              continue;

            var attractEvent = new PreAttractItemEvent(player, item.getItemStack());

            Bukkit.getPluginManager().callEvent(attractEvent);

            if (attractEvent.isCancelled() || !attractEvent.canHoldSome())
              continue;
          }

          else if (!(nearbyEntity instanceof ExperienceOrb))
            continue;

          perTickAttractionSessionByEntityId
            .computeIfAbsent(nearbyEntity.getEntityId(), k -> new EntityAttractionSession(nearbyEntity))
            .attractIfClosest(nearbyEntity, playerLocation);
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
  public void onPreAttractItem(PreAttractItemEvent event) {
    var inventory = event.getPlayer().getInventory();
    var attractedItem = event.getAttractedItem();

    for (var slotIndex = 0; slotIndex < 9 * 4; ++slotIndex) {
      var currentItem = inventory.getItem(slotIndex);

      if (currentItem == null || currentItem.getType().isAir()) {
        event.markCanHoldSome();
        return;
      }

      if (!attractedItem.isSimilar(currentItem))
        continue;

      var remainingSpace = currentItem.getMaxStackSize() - currentItem.getAmount();

      if (remainingSpace <= 0)
        continue;

      event.markCanHoldSome();
      return;
    }
  }

  private void loadWorldsFromConfig() {
    worlds.clear();

    for (var worldName : config.rootSection.invMagnet.worlds) {
      var world = Bukkit.getWorld(worldName);

      if (world == null) {
        logger.warning("Could not find world for item-magnet called \"" + worldName + "\"");
        continue;
      }

      worlds.add(world);
    }
  }
}

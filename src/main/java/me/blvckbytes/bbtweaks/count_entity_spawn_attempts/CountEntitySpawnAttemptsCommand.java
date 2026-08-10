package me.blvckbytes.bbtweaks.count_entity_spawn_attempts;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CountEntitySpawnAttemptsCommand implements CommandHandler, Tickable, Listener {

  private static final int COUNT_DURATION_S = 10;

  private final PluginCommand command;

  private final Map<UUID, SpawnAttemptCountSession> activeCountSessionByWorldId;

  private long relativeTime;

  public CountEntitySpawnAttemptsCommand(
    JavaPlugin plugin
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand("countentityspawnattempts"));

    this.activeCountSessionByWorldId = new HashMap<>();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return null;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender))
      return false;

    if (args.length != 1) {
      sender.sendMessage("§c[SpawnAttempts] Benutzung: /" + label + " <Welt>");
      return true;
    }

    var worldName = args[0];
    var world = Bukkit.getWorld(worldName);

    if (world == null) {
      sender.sendMessage("§c[SpawnAttempts] Unbekannte Welt: " + worldName);
      return true;
    }

    if (activeCountSessionByWorldId.containsKey(world.getUID())) {
      sender.sendMessage("§c[SpawnAttempts] Für die Welt " + world.getName() + " ist bereits ein Zählprozess im Gange!");
      return true;
    }

    activeCountSessionByWorldId.put(world.getUID(), new SpawnAttemptCountSession(relativeTime, world));

    broadcastToAuthorized("§a[SpawnAttempts] Zählprozess für die Welt " + world.getName() + " für " + COUNT_DURATION_S + "s gestartet.");
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(command.testPermission(sender)))
      return List.of();

    if (args.length == 1) {
      return Bukkit.getWorlds().stream()
        .map(World::getName)
        .filter(name -> StringUtils.startsWithIgnoreCase(name, args[0]))
        .toList();
    }

    return List.of();
  }

  @EventHandler
  public void onPreSpawn(PreCreatureSpawnEvent event) {
    if (event.getReason() != CreatureSpawnEvent.SpawnReason.NATURAL)
      return;

    var countSession = activeCountSessionByWorldId.get(event.getSpawnLocation().getWorld().getUID());

    if (countSession == null)
      return;

    var typeCount = countSession.counts.computeIfAbsent(event.getType(), _ -> new MutableInt());

    typeCount.value++;
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    for (var valueIterator = activeCountSessionByWorldId.values().iterator(); valueIterator.hasNext();) {
      var countSession = valueIterator.next();
      var sessionAge = relativeTime - countSession.startStamp;

      if (sessionAge < COUNT_DURATION_S * 20)
        continue;

      valueIterator.remove();

      var resultsString = "§cKeine Spawnversuche gemessen";

      if (!countSession.counts.isEmpty()) {
        resultsString = countSession.counts.entrySet().stream()
          .map(entry -> entry.getValue().value + "x " + entry.getKey().name())
          .collect(Collectors.joining(", "));
      }

      broadcastToAuthorized("§a[SpawnAttempts] Zählprozess der Welt " + countSession.world.getName() + " beendet: " + resultsString + ".");
    }
  }

  private void broadcastToAuthorized(String message) {
    var permission = command.getPermission();

    for (var player : Bukkit.getOnlinePlayers()) {
      if (permission == null || player.hasPermission(permission))
        player.sendMessage(message);
    }

    Bukkit.getConsoleSender().sendMessage(message);
  }
}

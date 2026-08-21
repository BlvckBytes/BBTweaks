package me.blvckbytes.bbtweaks.locate_entities;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LocateEntitiesCommand implements CommandHandler {

  private final PluginCommand command;

  private final Plugin plugin;

  private @Nullable EntityScanSession scanSession;

  public LocateEntitiesCommand(
    JavaPlugin plugin
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand("locateentities"));

    this.plugin = plugin;
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
    if (!hasCommandPermission(sender))
      return false;

    if (this.scanSession != null) {
      sender.sendMessage("§cThere's already an active scan-session ongoing - please wait!");
      return true;
    }

    if (args.length != 2) {
      sender.sendMessage("§cUsage: /" + label + " <entity-type> <world>");
      return true;
    }

    EntityType type;

    try {
      type = EntityType.valueOf(args[0].toUpperCase());
    } catch (Throwable e) {
      sender.sendMessage("§cUnknown entity-type: §4" + args[0]);
      return true;
    }

    var world = Bukkit.getWorld(args[1]);

    if (world == null) {
      sender.sendMessage("§cUnknown world: §4" + args[1]);
      return true;
    }

    this.scanSession = new EntityScanSession(world, type);

    scanSession.run(plugin, () -> {
      var session = this.scanSession;
      this.scanSession = null;

      displayResults(session, sender);
    });

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!hasCommandPermission(sender))
      return List.of();

    if (args.length == 1) {
      return Arrays.stream(EntityType.values())
        .map(Enum::name)
        .filter(name -> StringUtils.startsWithIgnoreCase(name, args[0]))
        .limit(10)
        .toList();
    }

    if (args.length == 2) {
      return Bukkit.getWorlds().stream()
        .map(World::getName)
        .filter(name -> StringUtils.startsWithIgnoreCase(name, args[1]))
        .toList();
    }

    return List.of();
  }

  private void displayResults(EntityScanSession scanSession, CommandSender sender) {
    var clusters = ChunkCluster.findClusters(scanSession.countByChunkTuple);

    if (clusters.isEmpty()) {
      sender.sendMessage("§cNo entities of type §4" + scanSession.entityType.name() + " §care currently loaded in §4" + scanSession.world.getName());
      return;
    }

    sender.sendMessage("§aChunk-Cluster for type " + scanSession.entityType.name() + " of world " + scanSession.world.getName() + ":");

    for (var index = 0; index < clusters.size(); ++index) {
      if (index == 10) {
        sender.sendMessage("§eStopped printing after 10 lines, had " + (clusters.size() - index) + " more");
        break;
      }

      var cluster = clusters.get(index);
      var x = IntTuple.getFirst(cluster.highestCountChunk());
      var z = IntTuple.getSecond(cluster.highestCountChunk());

      sender.sendMessage("§a- x=" + x*16 + " z=" + z*16 + " (" + cluster.chunks().size() + " chunks): " + cluster.totalCount() + "x " + scanSession.entityType.name());
    }
  }
}

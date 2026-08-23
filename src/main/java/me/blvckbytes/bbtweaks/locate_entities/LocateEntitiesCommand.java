package me.blvckbytes.bbtweaks.locate_entities;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.util.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.stream.Stream;

public class LocateEntitiesCommand implements CommandHandler {

  private static final String COUNTS_SENTINEL = "counts";
  private static final long MAX_RESULT_LINES = 15;

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
      sender.sendMessage("§cUsage: /" + label + " <world> <entity-type|" + COUNTS_SENTINEL + ">");
      return true;
    }

    var world = Bukkit.getWorld(args[0]);

    if (world == null) {
      sender.sendMessage("§cUnknown world: §4" + args[0]);
      return true;
    }

    EntityType type = null;

    var scanForCounts = args[1].equalsIgnoreCase(COUNTS_SENTINEL);

    if (!scanForCounts) {
      try {
        type = EntityType.valueOf(args[1].toUpperCase());
      } catch (Throwable e) {
        sender.sendMessage("§cUnknown entity-type: §4" + args[1]);
        return true;
      }
    }

    this.scanSession = new EntityScanSession(world, type);

    scanSession.run(plugin, () -> {
      var session = this.scanSession;
      this.scanSession = null;

      if (scanForCounts) {
        displayTotalCounts(session, sender);
        return;
      }

      displayClusters(session, sender);
    });

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!hasCommandPermission(sender))
      return List.of();

    if (args.length == 1) {
      return Bukkit.getWorlds().stream()
        .map(World::getName)
        .filter(name -> StringUtils.startsWithIgnoreCase(name, args[0]))
        .toList();
    }

    if (args.length == 2) {
      return Stream.concat(
          Arrays.stream(EntityType.values())
            .map(Enum::name),
          Stream.of(COUNTS_SENTINEL)
        )
        .filter(name -> StringUtils.startsWithIgnoreCase(name, args[1]))
        .limit(10)
        .toList();
    }

    return List.of();
  }

  private void displayTotalCounts(EntityScanSession scanSession, CommandSender sender) {
    if (scanSession.totalCountByType == null)
      throw new IllegalStateException("Cannot display total counts without the corresponding map being present");

    if (scanSession.totalCountByType.isEmpty()) {
      sender.sendMessage("§cThere are no entities currently loaded in §4\"" + scanSession.world.getName() + "\"");
      return;
    }

    record TypeAndCount(EntityType type, int count) {}

    var counts = new ArrayList<TypeAndCount>();

    for (var entry : scanSession.totalCountByType.entrySet())
      counts.add(new TypeAndCount(entry.getKey(), entry.getValue().value));

    counts.sort((a, b) -> -Integer.compare(a.count, b.count));

    sender.sendMessage("§aTotal entity-counts of world \"" + scanSession.world.getName() + "\":");

    for (var index = 0; index < counts.size(); ++index) {
      if (index == MAX_RESULT_LINES) {
        sender.sendMessage("§eStopped printing after 10 lines, had " + (counts.size() - index) + " more");
        break;
      }

      var typeAndCount = counts.get(index);

      sender.sendMessage("§a- " + typeAndCount.count + "x " + typeAndCount.type.name());
    }
  }

  private void displayClusters(EntityScanSession scanSession, CommandSender sender) {
    if (scanSession.countByChunkTuple == null || scanSession.targetEntityType == null)
      throw new IllegalStateException("Cannot display clusters without per-chunk counts being present");

    var clusters = ChunkCluster.findClusters(scanSession.countByChunkTuple);

    if (clusters.isEmpty()) {
      sender.sendMessage("§cNo entities of type §4" + scanSession.targetEntityType.name() + " §care currently loaded in world \"§4" + scanSession.world.getName() + "\"");
      return;
    }

    sender.sendMessage("§aChunk-Cluster for type " + scanSession.targetEntityType.name() + " of world \"" + scanSession.world.getName() + "\":");

    for (var index = 0; index < clusters.size(); ++index) {
      if (index == MAX_RESULT_LINES) {
        sender.sendMessage("§eStopped printing after 10 lines, had " + (clusters.size() - index) + " more");
        break;
      }

      var cluster = clusters.get(index);
      var x = IntTuple.getFirst(cluster.highestCountChunk()) * 16;
      var z = IntTuple.getSecond(cluster.highestCountChunk()) * 16;

      var safeTarget = TeleportUtil.findSafeTeleportLocation(scanSession.world, x, z);

      var y = safeTarget == null ? 50 : safeTarget.getBlockY();

      sender.sendMessage(
        Component.text("- x=" + x + " z=" + z + " (" + cluster.chunks().size() + " chunks): " + cluster.totalCount() + "x " + scanSession.targetEntityType.name())
          .color(NamedTextColor.GREEN)
          .hoverEvent(
            HoverEvent.showText(
              Component.text("Click to teleport to chunk")
                .color(NamedTextColor.GREEN)
            )
          )
          .clickEvent(ClickEvent.runCommand("tppos " + x + " " + y + " " + z))
      );
    }
  }
}

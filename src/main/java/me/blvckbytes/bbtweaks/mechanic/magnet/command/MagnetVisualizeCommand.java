package me.blvckbytes.bbtweaks.mechanic.magnet.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.mechanic.magnet.MagnetInstance;
import me.blvckbytes.bbtweaks.mechanic.magnet.MagnetMechanic;
import me.blvckbytes.bbtweaks.mechanic.util.IntTuple;
import me.blvckbytes.bbtweaks.util.CacheByPosition;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MagnetVisualizeCommand implements CommandHandler {

  private static final class MagnetCollectionOutput {
    final List<MagnetInstance> instances;
    final LongSet seenChunkXZTuples;
    final LongSet seenMagnets;

    MagnetCollectionOutput() {
      instances = new ArrayList<>();
      seenChunkXZTuples = new LongOpenHashSet();
      seenMagnets = new LongOpenHashSet();
    }

    boolean addSeenChunk(int chunkX, int chunkZ) {
      return seenChunkXZTuples.add(IntTuple.create(chunkX, chunkZ));
    }

    boolean addSeenMagnet(MagnetInstance instance) {
      var sign = instance.getSign();
      return seenMagnets.add(CacheByPosition.computeWorldlessBlockId(sign.getX(), sign.getY(), sign.getZ()));
    }
  }

  private final PluginCommand command;
  private final MagnetMechanic magnetMechanic;
  private final ConfigKeeper<MainSection> config;

  public MagnetVisualizeCommand(
    JavaPlugin plugin,
    MagnetMechanic magnetMechanic,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(MagnetVisualizeCommandSection.INITIAL_NAME));
    this.magnetMechanic = magnetMechanic;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.mechanic.magnet.visualizeCommandPlayersOnly.sendMessage(sender);
      return true;
    }

    var origin = player.getLocation();
    var output = new MagnetCollectionOutput();

    collectNearbyAccessibleMagnets(player, origin.getBlockX() >> 4, origin.getBlockZ() >> 4, output);

    if (output.instances.isEmpty()) {
      config.rootSection.mechanic.magnet.visualizeCommandNoMagnetsNearby.sendMessage(sender);
      return true;
    }

    for (var instance : output.instances)
      magnetMechanic.visualizeInstance(player, instance, false);

    config.rootSection.mechanic.magnet.visualizeCommandVisualizingNearbyMagnets.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("visualization_duration", config.rootSection.mechanic.magnet.visualization.durationMs / 1000)
        .withVariable("magnet_count", output.instances.size())
    );

    return true;
  }

  private void collectNearbyAccessibleMagnets(Player viewer, int chunkX, int chunkZ, MagnetCollectionOutput output) {
    var magnetsInChunk = magnetMechanic.getMagnetsInChunk(viewer.getWorld(), chunkX, chunkZ);

    var collectedCount = 0;

    for (var instance : magnetsInChunk) {
      if (!output.addSeenMagnet(instance))
        continue;

      if (!magnetMechanic.canEditSign(viewer, instance.getSign()))
        continue;

      output.instances.add(instance);
      ++collectedCount;
    }

    if (collectedCount == 0)
      return;

    var radius = config.rootSection.mechanic.magnet.visualizeCommandChunkExpandRadius;

    for (var dx = -radius; dx <= radius; ++dx) {
      for (var dz = -radius; dz <= radius; ++dz) {
        if (dx == 0 && dz == 0)
          continue;

        var nextChunkX = chunkX + dx;
        var nextChunkZ = chunkZ + dz;

        if (!output.addSeenChunk(nextChunkX, nextChunkZ))
          continue;

        collectNearbyAccessibleMagnets(viewer, nextChunkX, nextChunkZ, output);
      }
    }
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.mechanic.magnet.visualizeCommand;
  }
}

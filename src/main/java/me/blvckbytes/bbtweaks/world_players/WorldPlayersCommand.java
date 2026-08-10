package me.blvckbytes.bbtweaks.world_players;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class WorldPlayersCommand implements CommandHandler {

  private final PluginCommand command;

  private final ConfigKeeper<MainSection> config;

  public WorldPlayersCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(WorldPlayersCommandSection.INITIAL_NAME));

    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.worldPlayersCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    var counts = new LinkedHashMap<String, Integer>();

    for (var world : Bukkit.getWorlds()) {
      var playerCount = world.getPlayerCount();

      if (playerCount > 0)
        counts.put(world.getName(), playerCount);
    }

    if (counts.isEmpty()) {
      config.rootSection.worldPlayersCommand.noPlayersOnline.sendMessage(sender);
      return true;
    }

    config.rootSection.worldPlayersCommand.playerCountsOverview.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("count_map", counts)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }
}

package me.blvckbytes.bbtweaks.list_chunk_tickets;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ListChunkTicketsCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;

  public ListChunkTicketsCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(ListChunkTicketsCommandSection.INITIAL_NAME));

    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.listChunkTickets.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.listChunkTickets.notAPlayer.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.listChunkTickets.noPermission.sendMessage(sender);
      return true;
    }

    var world = player.getWorld();
    var loadedChunks = world.getLoadedChunks();
    var existingTickets = new ArrayList<ExistingChunkTicket>();

    for (var chunk : loadedChunks) {
      var plugins = chunk.getPluginChunkTickets();

      if (plugins.isEmpty())
        continue;

      var pluginNames = new HashSet<String>();

      for (var plugin : plugins)
        pluginNames.add(plugin.getName());

      existingTickets.add(new ExistingChunkTicket(chunk, pluginNames));
    }

    if (existingTickets.isEmpty()) {
      config.rootSection.listChunkTickets.noActiveTickets.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("world", world.getName())
      );

      return true;
    }

    config.rootSection.listChunkTickets.ticketListScreen.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("world", world.getName())
        .withVariable("tickets", existingTickets)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }
}

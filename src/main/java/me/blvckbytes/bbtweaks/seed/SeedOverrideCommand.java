package me.blvckbytes.bbtweaks.seed;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SeedOverrideCommand implements CommandHandler, Listener {

  private final PluginCommand command;

  private final ConfigKeeper<MainSection> config;

  public SeedOverrideCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand("seed"));

    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.seedOverride.playersOnly.sendMessage(sender);
      return true;
    }

    var currentWorld = player.getWorld();

    var message = config.rootSection.seedOverride._worldSpecificMessageByNameLower.getOrDefault(
      currentWorld.getName().toLowerCase(),
      config.rootSection.seedOverride.fallbackMessage
    );

    message.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("world", currentWorld.getName())
        .withVariable("seed", currentWorld.getSeed())
    );


    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreProcess(PlayerCommandPreprocessEvent event) {
    var message = event.getMessage();
    var firstSpaceIndex = message.indexOf(' ');

    if (firstSpaceIndex < 0) {
      if (message.equals("/seed"))
        event.setMessage("/bbtweaks:seed");

      return;
    }

    var commandToken = message.substring(0, firstSpaceIndex);

    if (commandToken.equals("/seed"))
      event.setMessage("/bbtweaks:seed" + message.substring(firstSpaceIndex));
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return null;
  }
}

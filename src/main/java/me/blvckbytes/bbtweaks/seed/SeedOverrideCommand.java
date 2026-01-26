package me.blvckbytes.bbtweaks.seed;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SeedOverrideCommand implements CommandExecutor, TabCompleter, Listener {

  private final ConfigKeeper<MainSection> config;

  public SeedOverrideCommand(ConfigKeeper<MainSection> config) {
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
}

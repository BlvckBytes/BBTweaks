package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.homes.storage.HomesStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class HomesCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final HomesStorage homesStorage;

  public HomesCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HomesStorage homesStorage
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(HomesCommandSection.INITIAL_NAME));
    this.config = config;
    this.homesStorage = homesStorage;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.homes.homesCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.homes.playersOnly.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      var playerHomes = homesStorage.accessHomes(player);
      player.sendMessage("§aTODO: Open UI for sender");
      return true;
    }

    var targetPlayer = homesStorage.getKnownPlayerByName(args[0]);

    if (targetPlayer == null) {
      config.rootSection.homes.targetPlayerNameIsUnknown.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", args[0])
      );

      return true;
    }

    var playerHomes = homesStorage.accessHomes(targetPlayer);
    player.sendMessage("§aTODO: Open UI for target");
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    return List.of();
  }
}

package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.homes.storage.HomesStorage;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DelHomeCommand extends HomeCommandBase {

  public DelHomeCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HomesStorage homesStorage
  ) {
    super(plugin, config, homesStorage, DelHomeCommandSection.INITIAL_NAME, section -> section.delHomeCommand);
  }

  @Override
  protected void onHomeNameParsedCommand(@NotNull Player sender, @NotNull Command command, @NotNull String label, @Nullable HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    if (homeParameter == null) {
      config.rootSection.homes.delHomeCommand.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return;
    }

    var playerHomes = homesStorage.accessHomes(homeParameter.target());
    var deletedHome = playerHomes.deleteHomeIfExists(homeParameter.homeName());

    if (deletedHome == null) {
      config.rootSection.homes.delHomeCommand.homeDoesNotExist.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("target", homeParameter.target().playerId().equals(sender.getUniqueId()) ? null : homeParameter.target().lastKnownName())
          .withVariable("name", homeParameter.homeName())
      );

      return;
    }

    config.rootSection.homes.delHomeCommand.homeDeleted.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("home", deletedHome.makeEnvironment(config))
        .withVariable("target", homeParameter.target().playerId().equals(sender.getUniqueId()) ? null : homeParameter.target().lastKnownName())
    );
  }

  @Override
  public @Nullable List<String> onHomeNameParsedTabComplete(@NotNull Player sender, @NotNull Command command, @NotNull String label, @NotNull HomeParameter homeParameter, @NotNull String @NotNull [] args) {
    return List.of();
  }
}

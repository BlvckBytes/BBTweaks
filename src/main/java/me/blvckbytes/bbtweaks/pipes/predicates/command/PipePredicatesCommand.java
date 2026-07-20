package me.blvckbytes.bbtweaks.pipes.predicates.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.search.command.PipeSearchCommand;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipePredicatesCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final IPPIntegration ippIntegration;
  private final PipeSearchCommand pipeSearchCommand;
  private final ConfigKeeper<MainSection> config;

  public PipePredicatesCommand(
    JavaPlugin plugin,
    IPPIntegration ippIntegration,
    PipeSearchCommand pipeSearchCommand,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(PipePredicatesCommandSection.INITIAL_NAME));

    this.ippIntegration = ippIntegration;
    this.pipeSearchCommand = pipeSearchCommand;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.pipes.predicates.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.pipes.predicates.command.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.pipes.predicates.command.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.pipes.predicates.command.actionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    // Keep these aliases around, as they're already broadly known on our server.
    switch (normalizedAction.constant) {
      case SET, SET_LANGUAGE, GET, REMOVE:
        return ippIntegration.mainCommand.getExecutor().onCommand(sender, command, label, args);
    }

    // TODO: Generate
    // TODO: Visualize/Clear-Visualize?
    // TODO: Locate-Predicates?

    // Keep this alias around, as it's already broadly known on our server.
    if (normalizedAction.constant == CommandAction.SEARCH)
      return pipeSearchCommand.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));

    throw new IllegalStateException("Unaccounted-for command-action: " + normalizedAction.constant.name());
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(player) || args.length == 0)
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (normalizedAction.constant == CommandAction.SEARCH)
      return pipeSearchCommand.onTabComplete(sender, pipeSearchCommand.getCommand(), pipeSearchCommand.getCommand().getLabel(), Arrays.copyOfRange(args, 1, args.length));

    // Keep these aliases around, as they're already broadly known on our server
    switch (normalizedAction.constant) {
      case SET, SET_LANGUAGE, GET, REMOVE: {
        if (ippIntegration.mainCommand.getExecutor() instanceof TabCompleter tabCompleter)
          return tabCompleter.onTabComplete(sender, command, label, args);
      }
    }

    return List.of();
  }
}

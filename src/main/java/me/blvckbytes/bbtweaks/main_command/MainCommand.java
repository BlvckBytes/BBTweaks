package me.blvckbytes.bbtweaks.main_command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.RDBreakTool;
import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainCommand implements CommandExecutor, TabExecutor {

  private enum Action implements MatchableEnum {
    RELOAD,
    RD_BREAKER,
    ;

    static final EnumMatcher<Action> matcher = new EnumMatcher<>(values());
  }

  private final ConfigKeeper<MainSection> config;
  private final RDBreakTool rdBreakTool;
  private final Logger logger;

  public MainCommand(
    ConfigKeeper<MainSection> config,
    RDBreakTool rdBreakTool,
    Logger logger
  ) {
    this.config = config;
    this.rdBreakTool = rdBreakTool;
    this.logger = logger;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("bbtweaks.command")) {
      config.rootSection.mainCommand.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<Action> action;

    if (args.length == 0 || (action = Action.matcher.matchFirst(args[0])) == null) {
      config.rootSection.mainCommand.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("command_label", label)
          .withVariable("actions", Action.matcher.createCompletions(null))
      );

      return true;
    }

    switch (action.constant) {
      case RELOAD -> {
        try {
          config.reload();
          config.rootSection.mainCommand.configReloadSuccess.sendMessage(sender);
        } catch (Exception e) {
          config.rootSection.mainCommand.configReloadError.sendMessage(sender);
          logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);
        }
        return true;
      }

      case RD_BREAKER -> {
        if (!(sender instanceof Player player)) {
          config.rootSection.mainCommand.setRdBreakerPlayersOnly.sendMessage(sender);
          return false;
        }

        var heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType().isAir()) {
          config.rootSection.mainCommand.setRdBreakerNoValidItem.sendMessage(sender);
          return true;
        }

        rdBreakTool.modifyItemToBecomeRdBreaker(heldItem);

        config.rootSection.mainCommand.setRdBreakerMetadata.sendMessage(sender);
        return true;
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("bbtweaks.command") || args.length != 1)
      return List.of();

    return Action.matcher.createCompletions(args[0]);
  }
}

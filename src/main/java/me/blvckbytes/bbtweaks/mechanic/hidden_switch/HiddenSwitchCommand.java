package me.blvckbytes.bbtweaks.mechanic.hidden_switch;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;

public class HiddenSwitchCommand implements CommandExecutor, TabCompleter {

  private final HiddenSwitchMechanic mechanic;
  private final ConfigKeeper<MainSection> config;

  public HiddenSwitchCommand(
    HiddenSwitchMechanic mechanic,
    ConfigKeeper<MainSection> config
  ) {
    this.mechanic = mechanic;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.mechanic.hiddenSwitch.commandPlayerOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.mechanic.hidden-switch")) {
      config.rootSection.mechanic.hiddenSwitch.noPermission.sendMessage(player);
      return true;
    }

    NormalizedConstant<CommandAction> action;

    if (args.length == 0 || (action = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.mechanic.hiddenSwitch.commandUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );
      return true;
    }

    var instance = mechanic.getLookedAtInstance(player);

    if (instance == null) {
      config.rootSection.mechanic.hiddenSwitch.commandNotLookingAtInstance.sendMessage(player);
      return true;
    }

    if (!mechanic.canEditSign(player, instance.getSign())) {
      config.rootSection.mechanic.hiddenSwitch.cannotEditSign.sendMessage(player);
      return true;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", instance.getSign().getX())
      .withVariable("y", instance.getSign().getY())
      .withVariable("z", instance.getSign().getZ());

    switch (action.constant) {
      case GET_PASSWORD -> {
        if (instance.password == null) {
          config.rootSection.mechanic.hiddenSwitch.commandGetPasswordNone.sendMessage(player, environment);
          return true;
        }

        config.rootSection.mechanic.hiddenSwitch.commandGetPassword.sendMessage(
          player,
          environment
            .withVariable("password", instance.password)
        );
        return true;
      }

      case SET_PASSWORD -> {
        var passwordBuilder = new StringJoiner(" ");

        for (var argIndex = 1; argIndex < args.length; ++argIndex)
          passwordBuilder.add(args[argIndex]);

        var password = passwordBuilder.toString();

        if (password.isBlank()) {
          config.rootSection.mechanic.hiddenSwitch.commandSetPasswordMissingValue.sendMessage(player);
          return true;
        }

        mechanic.updatePasswordAndGetPriorValue(instance, password);

        config.rootSection.mechanic.hiddenSwitch.commandSetPassword.sendMessage(
          player,
          environment
            .withVariable("password", password)
        );

        return true;
      }

      case REMOVE_PASSWORD -> {
        var priorValue = mechanic.updatePasswordAndGetPriorValue(instance, null);

        if (priorValue == null) {
          config.rootSection.mechanic.hiddenSwitch.commandRemoveNoneSet.sendMessage(player, environment);
          return true;
        }

        config.rootSection.mechanic.hiddenSwitch.commandRemovePassword.sendMessage(
          player,
          environment
            .withVariable("password", priorValue)
        );

        return true;
      }

      case ALLOW_KEY_OR_PASSWORD -> {
        var newValue = mechanic.toggleAllowKeyOrPasswordAndGetNewValue(instance);

        if (newValue) {
          config.rootSection.mechanic.hiddenSwitch.commandEnableKeyOrPassword.sendMessage(player, environment);
          return true;
        }

        config.rootSection.mechanic.hiddenSwitch.commandDisableKeyOrPassword.sendMessage(player, environment);
        return true;
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !player.hasPermission("bbtweaks.mechanic.hidden-switch"))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }
}

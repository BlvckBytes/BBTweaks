package me.blvckbytes.bbtweaks.inv_magnet.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.inv_magnet.config.InvMagnetCommandSection;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InvMagnetCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final InvMagnetParametersStore parametersStore;
  private final ConfigKeeper<MainSection> config;

  public InvMagnetCommand(
    JavaPlugin plugin,
    InvMagnetParametersStore parametersStore,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(InvMagnetCommandSection.INITIAL_NAME));

    this.parametersStore = parametersStore;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.invMagnet.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.invmagnet")) {
      config.rootSection.invMagnet.missingPermission.sendMessage(player);
      return true;
    }

    var parameter = parametersStore.accessParameters(player);
    var limits = parameter.updateLimitsAndConstrain();

    if (limits == null) {
      config.rootSection.invMagnet.currentlyHasNoAccess.sendMessage(player);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.invMagnet.actionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );
      return true;
    }

    switch (normalizedAction.constant) {
      case ON -> parameter.setEnabledAndMessage(player, true);
      case OFF -> parameter.setEnabledAndMessage(player, false);
      case TOGGLE -> parameter.setEnabledAndMessage(player, null);
      case RADIUS -> {
        if (args.length != 2) {
          config.rootSection.invMagnet.radiusUsage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        var newRadius = -1;

        try {
          newRadius = Integer.parseInt(args[1]);
        } catch (Throwable ignored) {}

        if (newRadius < 0) {
          config.rootSection.invMagnet.invalidRadius.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("input", args[1])
          );

          return true;
        }

        if (parameter.setRadiusAndGetIfExceeded(newRadius)) {
          config.rootSection.invMagnet.exceededRadiusLimit.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("radius", newRadius)
              .withVariable("radius_limit", parameter.getLimits().maxRadius())
          );
        }

        config.rootSection.invMagnet.updatedRadius.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("radius", parameter.getRadius())
            .withVariable("world_group_name", limits.worldGroupDisplayName())
        );

        if (!parameter.isEnabled())
          parameter.setEnabledAndMessage(player, true);
      }
      case STATUS -> {
        config.rootSection.invMagnet.status.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("radius", parameter.getRadius())
            .withVariable("enabled", parameter.isEnabled())
            .withVariable("world_group_name", limits.worldGroupDisplayName())
        );
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !sender.hasPermission("bbtweaks.invmagnet"))
      return List.of();

    var parameter = parametersStore.accessParameters(player);

    if (parameter.updateLimitsAndConstrain() == null)
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var action = CommandAction.matcher.matchFirst(args[0]);

    if (action != null) {
      if (action.constant == CommandAction.RADIUS && args.length == 2) {
        var radii = new ArrayList<String>();

        for (var radius = 1; radius <= parameter.getLimits().maxRadius(); ++radius) {
          var radiusString = String.valueOf(radius);

          if (radiusString.startsWith(args[1]))
            radii.add(radiusString);
        }

        return radii;
      }
    }

    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.invMagnet.command;
  }
}

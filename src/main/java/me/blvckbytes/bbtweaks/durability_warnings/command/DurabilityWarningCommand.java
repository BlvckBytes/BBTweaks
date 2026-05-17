package me.blvckbytes.bbtweaks.durability_warnings.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.durability_warnings.WarningsProfileStore;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DurabilityWarningCommand implements CommandExecutor, TabCompleter {

  private final WarningsProfileStore profileStore;
  private final ConfigKeeper<MainSection> config;

  public DurabilityWarningCommand(
    WarningsProfileStore profileStore,
    ConfigKeeper<MainSection> config
  ) {
    this.profileStore = profileStore;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.durabilityWarnings.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(sender)) {
      config.rootSection.durabilityWarnings.missingPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length != 1 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.durabilityWarnings.commandActionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    var profile = profileStore.accessProfile(player);

    switch (normalizedAction.constant) {
      case TOGGLE_ENABLED -> {
        var newState = profile.enabled ^= true;

        config.rootSection.durabilityWarnings.toggleEnabled.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("state", newState)
        );
      }

      case TOGGLE_SOUND -> {
        var newState = profile.playSound ^= true;

        config.rootSection.durabilityWarnings.toggleSound.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("state", newState)
        );
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player) || args.length != 1 || !command.testPermission(sender))
      return List.of();

    return CommandAction.matcher.createCompletions(args[0]);
  }
}

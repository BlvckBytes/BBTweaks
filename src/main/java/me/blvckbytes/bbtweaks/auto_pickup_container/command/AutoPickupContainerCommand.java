package me.blvckbytes.bbtweaks.auto_pickup_container.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_pickup_container.settings.AutoPickupContainerSettingsStore;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AutoPickupContainerCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final AutoPickupContainerSettingsStore settingsStore;

  public AutoPickupContainerCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    AutoPickupContainerSettingsStore settingsStore
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(AutoPickupContainerCommandSection.INITIAL_NAME));
    this.config = config;
    this.settingsStore = settingsStore;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.autoPickupContainer.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.autoPickupContainer.command.playersOnly.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> action;

    if (args.length != 1 || (action = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.autoPickupContainer.command.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    var settings = settingsStore.accessSettings(player);

    switch (action.constant) {
      case ENABLE -> settings.setEnabled(true);
      case DISABLE -> settings.setEnabled(false);
      case TOGGLE -> settings.setEnabled(null);
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }
}

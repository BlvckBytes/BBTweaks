package me.blvckbytes.bbtweaks.hotbar_randomizer.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.hotbar_randomizer.HotbarRandomizerSettingsStore;
import me.blvckbytes.bbtweaks.hotbar_randomizer.settings_display.HotbarRandomizerSettingsDisplayHandler;
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

public class HotbarRandomizerCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final HotbarRandomizerSettingsStore settingsStore;
  private final HotbarRandomizerSettingsDisplayHandler displayHandler;

  public HotbarRandomizerCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HotbarRandomizerSettingsStore settingsStore,
    HotbarRandomizerSettingsDisplayHandler displayHandler
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(HotbarRandomizerCommandSection.INITIAL_NAME));
    this.config = config;
    this.settingsStore = settingsStore;
    this.displayHandler = displayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.hotbarRandomizer.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.hotbarRandomizer.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.hotbarRandomizer.noPermission.sendMessage(sender);
      return true;
    }

    var settings = settingsStore.accessSettings(player);

    if (args.length == 0) {
      displayHandler.show(player, settings);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length != 1 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.hotbarRandomizer.commandActionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case ON -> settings.setEnabledAndSendMessage(true);
      case OFF -> settings.setEnabledAndSendMessage(false);
      case TOGGLE -> settings.setEnabledAndSendMessage(null);
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }
}

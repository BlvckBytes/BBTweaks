package me.blvckbytes.bbtweaks.sidebar.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesStore;
import me.blvckbytes.bbtweaks.sidebar.settings_display.SidebarSettingsDisplayHandler;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SidebarCommand implements CommandHandler {

  private final PluginCommand command;
  private final SidebarPreferencesStore sidebarPreferencesStore;
  private final SidebarSettingsDisplayHandler sidebarSettingsDisplayHandler;
  private final ConfigKeeper<MainSection> config;

  public SidebarCommand(
    JavaPlugin plugin,
    SidebarPreferencesStore sidebarPreferencesStore,
    SidebarSettingsDisplayHandler sidebarSettingsDisplayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(SidebarCommandSection.INITIAL_NAME));
    this.sidebarPreferencesStore = sidebarPreferencesStore;
    this.sidebarSettingsDisplayHandler = sidebarSettingsDisplayHandler;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender) || !(sender instanceof Player player))
      return false;

    var preferences = sidebarPreferencesStore.accessPreferences(player);

    if (args.length == 0) {
      preferences.toggleEnabled();
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      config.rootSection.sidebar.command.usage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    if (normalizedAction.constant == CommandAction.SETTINGS) {
      sidebarSettingsDisplayHandler.show(player, preferences);
      return true;
    }

    throw new IllegalStateException("Unaccounted-for command-action: " + normalizedAction.constant);
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender) || !(sender instanceof Player player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.sidebar.command;
  }
}

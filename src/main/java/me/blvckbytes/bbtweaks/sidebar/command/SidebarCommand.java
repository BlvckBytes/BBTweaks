package me.blvckbytes.bbtweaks.sidebar.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesStore;
import me.blvckbytes.bbtweaks.sidebar.settings_display.SidebarSettingsDisplayHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SidebarCommand implements CommandExecutor, TabCompleter {

  private final SidebarPreferencesStore sidebarPreferencesStore;
  private final SidebarSettingsDisplayHandler sidebarSettingsDisplayHandler;
  private final ConfigKeeper<MainSection> config;

  public SidebarCommand(
    SidebarPreferencesStore sidebarPreferencesStore,
    SidebarSettingsDisplayHandler sidebarSettingsDisplayHandler,
    ConfigKeeper<MainSection> config
  ) {
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
}

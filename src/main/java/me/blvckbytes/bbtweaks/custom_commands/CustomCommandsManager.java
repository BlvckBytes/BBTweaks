package me.blvckbytes.bbtweaks.custom_commands;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CustomCommandsManager {

  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;
  private final CommandUpdater commandUpdater;
  private final List<PluginCommand> registeredCommands;
  private final Constructor<PluginCommand> pluginCommandConstructor;

  public CustomCommandsManager(Plugin plugin, ConfigKeeper<MainSection> config) throws Exception {
    this.config = config;
    this.plugin = plugin;
    this.commandUpdater = new CommandUpdater(plugin);
    this.registeredCommands = new ArrayList<>();

    this.pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
    this.pluginCommandConstructor.setAccessible(true);

    config.registerReloadListener(this::updateCommands);
    updateCommands();
  }

  private void updateCommands() {
    for (var registeredCommand : registeredCommands)
      commandUpdater.tryUnregisterCommand(registeredCommand);

    registeredCommands.clear();

    for (var customCommand : config.rootSection.customCommands.commands) {
      var command = makeCommand(customCommand);

      if (command == null)
        continue;

      if (!commandUpdater.tryRegisterCommand(command)) {
        plugin.getLogger().log(Level.SEVERE, "Failed to register command /" + customCommand.evaluatedName);
        continue;
      }

      var executor = new CustomCommandExecutor(customCommand);

      command.setExecutor(executor);
      command.setTabCompleter(executor);

      registeredCommands.add(command);
    }

    for (var player : Bukkit.getOnlinePlayers())
      player.updateCommands();

    plugin.getLogger().info("Registered " + registeredCommands.size() + " custom-commands!");
  }

  private @Nullable PluginCommand makeCommand(CommandSection commandSection) {
    try {
      var command = pluginCommandConstructor.newInstance(commandSection.evaluatedName, plugin);
      command.setAliases(commandSection.evaluatedAliases);
      return command;
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to instantiate a plugin-command", e);
      return null;
    }
  }
}

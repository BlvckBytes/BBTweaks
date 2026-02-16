package me.blvckbytes.bbtweaks.custom_commands;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CustomCommandsManager implements Listener {

  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;
  private final CommandUpdater commandUpdater;
  private final List<PluginCommand> registeredCommands;
  private final Constructor<PluginCommand> pluginCommandConstructor;

  public CustomCommandsManager(
    CommandUpdater commandUpdater,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) throws Exception {
    this.config = config;
    this.plugin = plugin;
    this.commandUpdater = commandUpdater;
    this.registeredCommands = new ArrayList<>();

    this.pluginCommandConstructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
    this.pluginCommandConstructor.setAccessible(true);

    config.registerReloadListener(this::updateCommands);
    updateCommands();
  }

  @EventHandler
  public void onCommandSend(PlayerCommandSendEvent event) {
    event.getCommands().removeIf(config.rootSection.customCommands._hiddenCommandsLower::contains);
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

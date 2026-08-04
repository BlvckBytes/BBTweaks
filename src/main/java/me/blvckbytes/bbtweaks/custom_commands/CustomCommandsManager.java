package me.blvckbytes.bbtweaks.custom_commands;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
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

    Bukkit.getScheduler().runTaskLater(plugin, this::updateCommands, 1L);
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper == config)
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

    var customCommands = new ArrayList<CustomCommand>();

    for (var customCommandSection : config.rootSection.customCommands.commands)
      customCommands.add(new CustomCommandHandler(customCommandSection));

    var registerEvent = new RegisterAdditionalCustomCommandsEvent();
    Bukkit.getPluginManager().callEvent(registerEvent);

    customCommands.addAll(registerEvent.getCommands());

    for (var customCommand : customCommands) {
      var command = makeCommand(customCommand);

      if (command == null)
        continue;

      if (!commandUpdater.tryRegisterCommand(command)) {
        plugin.getLogger().log(Level.SEVERE, "Failed to register command /" + customCommand.getName());
        continue;
      }

      var executor = customCommand.getExecutor();

      command.setExecutor(executor);

      if (executor instanceof TabCompleter tabCompleter)
        command.setTabCompleter(tabCompleter);

      registeredCommands.add(command);
    }

    plugin.getLogger().info("Registered " + registeredCommands.size() + " custom-commands");
  }

  private @Nullable PluginCommand makeCommand(CustomCommand customCommand) {
    try {
      var command = pluginCommandConstructor.newInstance(customCommand.getName(), plugin);
      command.setAliases(customCommand.getAliases());
      return command;
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to instantiate a plugin-command", e);
      return null;
    }
  }
}

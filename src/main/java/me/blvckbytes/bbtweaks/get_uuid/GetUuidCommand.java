package me.blvckbytes.bbtweaks.get_uuid;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GetUuidCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;

  private final Set<String> knownNames;
  private final Map<String, UUID> idByNameLower;

  public GetUuidCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(GetUuidCommandSection.INITIAL_NAME));

    this.config = config;
    this.knownNames = new HashSet<>();
    this.idByNameLower = new HashMap<>();

    for (var offlinePlayer : Bukkit.getOfflinePlayers())
      addToKnown(offlinePlayer);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender.hasPermission("bbtweaks.getuuid"))) {
      config.rootSection.getUuid.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.getUuid.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("command_label", label)
      );

      return true;
    }

    var name = args[0];
    var uuid = idByNameLower.get(name.toLowerCase());

    if (uuid == null) {
      config.rootSection.getUuid.unknownName.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", name)
      );

      return true;
    }

    config.rootSection.getUuid.resultMessage.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("name", name)
        .withVariable("uuid", uuid.toString())
    );

    return true;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    addToKnown(event.getPlayer());
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender.hasPermission("bbtweaks.getuuid")))
      return List.of();

    if (args.length != 1)
      return List.of();

    return knownNames.stream()
      .filter(name -> StringUtils.startsWithIgnoreCase(name, args[0]))
      .toList();
  }

  private void addToKnown(OfflinePlayer player) {
    var name = player.getName();

    if (name == null)
      return;

    knownNames.add(name);
    idByNameLower.put(name.toLowerCase(), player.getUniqueId());
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.getUuid.command;
  }
}

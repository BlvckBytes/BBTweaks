package me.blvckbytes.bbtweaks;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GetUuidCommand implements CommandExecutor, TabCompleter, Listener {

  private final BBTweaksPlugin plugin;

  private final Set<String> knownNames;
  private final Map<String, UUID> idByNameLower;

  public GetUuidCommand(BBTweaksPlugin plugin) {
    this.plugin = plugin;
    this.knownNames = new HashSet<>();
    this.idByNameLower = new HashMap<>();

    for (var offlinePlayer : Bukkit.getOfflinePlayers())
      addToKnown(offlinePlayer);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender.hasPermission("bbtweaks.getuuid"))) {
      sender.sendMessage(plugin.accessConfigValue("chat.noCommandPermission"));
      return true;
    }

    if (args.length != 1) {
      sender.sendMessage(plugin.accessConfigValue("chat.getUuidUsage").replace("{command_label}", label));
      return true;
    }

    var name = args[0];
    var uuid = idByNameLower.get(name.toLowerCase());

    if (uuid == null) {
      sender.sendMessage(plugin.accessConfigValue("chat.getUuidUnknownName").replace("{name}", name));
      return true;
    }

    var resultMessage = plugin.accessConfigValue("chat.getUuidResult")
      .replace("{name}", name)
      .replace("{uuid}", uuid.toString());

    var hoverMessage = plugin.accessConfigValue("chat.getUuidResultHover");

    sender.spigot().sendMessage(
      new ComponentBuilder(resultMessage)
        .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid.toString()))
        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverMessage).create()))
        .create()
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
}

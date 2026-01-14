package me.blvckbytes.bbtweaks.ping;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PingCommand implements CommandExecutor, TabCompleter, Listener {

  private final Command command;
  private final ConfigKeeper<MainSection> config;

  public PingCommand(Command command, ConfigKeeper<MainSection> config) {
    this.command = command;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    var pingTarget = sender instanceof Player player ? player : null;

    if (args.length > 0) {
      pingTarget = Bukkit.getPlayer(args[0]);

      if (pingTarget == null) {
        config.rootSection.ping.targetNotOnline.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("name", args[0])
        );

        return true;
      }

      if (pingTarget != sender && !sender.hasPermission("bbtweaks.ping.other")) {
        config.rootSection.ping.noOtherPermission.sendMessage(sender);
        return true;
      }
    }

    if (pingTarget == null) {
      config.rootSection.ping.noTargetConsoleSender.sendMessage(sender);
      return true;
    }

    var pingOfTarget = pingTarget.getPing();
    var message = pingTarget == sender ? config.rootSection.ping.pingSelf : config.rootSection.ping.pingOther;

    message.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("ping", pingOfTarget)
        .withVariable("name", pingTarget.getName())
    );

    return true;
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length != 1 || !sender.hasPermission("bbtweaks.ping.other"))
      return List.of();

    return Bukkit.getOnlinePlayers().stream()
      .map(Player::getName)
      .filter(name -> StringUtils.startsWithIgnoreCase(name, args[0]) && !sender.getName().equals(name))
      .limit(10)
      .toList();
  }

  // Let's override Essentials *useless* ping-command.

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event) {
    event.setMessage(patchCommand(event.getMessage(), true));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onServerCommandPreProcess(ServerCommandEvent event) {
    event.setCommand(patchCommand(event.getCommand(), false));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTabCompleteEvent(TabCompleteEvent event) {
    var buffer = event.getBuffer();

    // The buffer always starts with a /, including server-commands

    if (!buffer.startsWith("/ping"))
      return;

    var tokens = buffer.split(" ");
    var args = tokens;

    if (buffer.endsWith(" ")) {
      for (var i = 1; i < args.length; ++i)
        args[i - 1] = args[i];

      args[args.length - 1] = "";
    }
    else {
      args = new String[args.length - 1];
      System.arraycopy(tokens, 1, args, 0, args.length);
    }

    var completions = onTabComplete(event.getSender(), command, "ping", args);

    if (completions != null)
      event.setCompletions(completions);
  }

  private String patchCommand(String command, boolean leadingSlash) {
    var targetCommand = (leadingSlash ? "/" : "") + "ping";
    var redirect = (leadingSlash ? "/" : "") + "bbtweaks:ping";

    var firstSpaceIndex = command.indexOf(' ');

    if (firstSpaceIndex < 0) {
      if (command.equals(targetCommand))
        return redirect;

      return command;
    }

    var commandToken = command.substring(0, firstSpaceIndex);

    if (commandToken.equals(targetCommand))
      return redirect + command.substring(firstSpaceIndex);

    return command;
  }
}

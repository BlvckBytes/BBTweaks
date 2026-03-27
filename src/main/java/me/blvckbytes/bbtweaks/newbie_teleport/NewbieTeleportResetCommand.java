package me.blvckbytes.bbtweaks.newbie_teleport;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NewbieTeleportResetCommand implements CommandExecutor, TabCompleter {

  private final ConfigKeeper<MainSection> config;
  private final NamespacedKey useCountKey;

  public NewbieTeleportResetCommand(NewbieTeleportCommand newbieTeleportCommand, ConfigKeeper<MainSection> config) {
    this.config = config;
    this.useCountKey = newbieTeleportCommand.useCountKey;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("bbtweaks.newbieteleport.reset")) {
      config.rootSection.newbieTeleport.missingPermissionResetCommand.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.newbieTeleport.usageResetCommand.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

    var target = Bukkit.getPlayer(args[0]);

    if (target == null) {
      config.rootSection.newbieTeleport.playerNotOnline.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", args[0])
      );

      return true;
    }

    var pdc = target.getPersistentDataContainer();
    var useCount = pdc.get(useCountKey, PersistentDataType.INTEGER);

    if (useCount == null || useCount <= 0) {
      config.rootSection.newbieTeleport.useCountAlreadyAtZero.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", target.getName())
      );

      return true;
    }

    pdc.set(useCountKey, PersistentDataType.INTEGER, 0);

    config.rootSection.newbieTeleport.useCountReset.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("name", target.getName())
        .withVariable("limit", config.rootSection.newbieTeleport.useCountLimit)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length == 1) {
      return Bukkit.getOnlinePlayers().stream()
        .filter(player -> StringUtils.startsWithIgnoreCase(player.getName(), args[0]))
        .limit(10)
        .map(Player::getName)
        .toList();
    }

    return List.of();
  }
}

package me.blvckbytes.bbtweaks.auto_wirer;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface CommandHandler extends CommandExecutor, TabCompleter {

  PluginCommand getCommand();

  @Nullable CommandSection getCommandSection();

  default String getShortestNameOrAlias() {
    var shortest = getCommand().getName();

    for (var alias : getCommand().getAliases()) {
      if (alias.length() < shortest.length())
        shortest = alias;
    }

    return shortest;
  }

  default boolean hasCommandSubPermission(Player player, String suffix) {
    var permission = getCommand().getPermission();

    if (permission == null)
      return true;

    return player.hasPermission(permission + "." + suffix);
  }

  default boolean hasCommandPermission(Player player) {
    var permission = getCommand().getPermission();

    if (permission == null)
      return true;

    return player.hasPermission(permission);
  }
}

package me.blvckbytes.bbtweaks.auto_wirer;

import at.blvckbytes.cm_mapper.section.command.CommandSection;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.Nullable;

public interface CommandHandler extends CommandExecutor, TabCompleter {

  PluginCommand getCommand();

  @Nullable CommandSection getCommandSection();

}

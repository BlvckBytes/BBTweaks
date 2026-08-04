package me.blvckbytes.bbtweaks.custom_commands;

import org.bukkit.command.CommandExecutor;

import java.util.List;

public interface CustomCommand {

  String getName();

  List<String> getAliases();

  CommandExecutor getExecutor();

}

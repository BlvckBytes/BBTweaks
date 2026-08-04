package me.blvckbytes.bbtweaks.custom_commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomCommandHandler implements CommandExecutor, TabCompleter, CustomCommand {

  private final CustomCommandSection commandsSection;

  public CustomCommandHandler(CustomCommandSection commandsSection) {
    this.commandsSection = commandsSection;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    commandsSection.message.sendMessage(sender);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @Override
  public String getName() {
    return commandsSection.evaluatedName;
  }

  @Override
  public List<String> getAliases() {
    return commandsSection.evaluatedAliases;
  }

  @Override
  public CommandExecutor getExecutor() {
    return this;
  }
}

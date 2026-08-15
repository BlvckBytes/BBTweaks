package me.blvckbytes.bbtweaks.emotions.command;

import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.custom_commands.CustomCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DirectEmotionCommand implements CustomCommand, CommandExecutor, TabCompleter {

  private final String commandName;
  private final String emotionIdentifier;
  private final CommandHandler emotionCommand;

  public DirectEmotionCommand(
    String commandName,
    String emotionIdentifier,
    CommandHandler emotionCommand
  ) {
    this.commandName = commandName;
    this.emotionIdentifier = emotionIdentifier;
    this.emotionCommand = emotionCommand;
  }

  @Override
  public String getName() {
    return commandName;
  }

  @Override
  public List<String> getAliases() {
    return List.of();
  }

  @Override
  public CommandExecutor getExecutor() {
    return this;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return emotionCommand.onCommand(sender, emotionCommand.getCommand(), emotionCommand.getShortestNameOrAlias(), prependIdentifier(args));
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return emotionCommand.onTabComplete(sender, emotionCommand.getCommand(), emotionCommand.getShortestNameOrAlias(), prependIdentifier(args));
  }

  private String[] prependIdentifier(String[] args) {
    var result = new String[args.length + 1];
    result[0] = emotionIdentifier;
    System.arraycopy(args, 0, result, 1, args.length);
    return result;
  }
}

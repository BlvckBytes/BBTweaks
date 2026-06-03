package me.blvckbytes.bbtweaks.sign_copier;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SignEditCommand implements CommandExecutor, TabCompleter {

  private final SignCopyCommand signCopyCommand;
  private final ConfigKeeper<MainSection> config;

  public SignEditCommand(
    SignCopyCommand signCopyCommand,
    ConfigKeeper<MainSection> config
  ) {
    this.signCopyCommand = signCopyCommand;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.signCopier.playersOnly.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      config.rootSection.signCopier.signEditUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );
      return true;
    }

    // For convenience, also offer a preview shortcut here
    if (args[0].equalsIgnoreCase(CommandAction.matcher.getNormalizedName(CommandAction.PREVIEW))) {
      signCopyCommand.handlePreviewAction(player);
      return true;
    }

    signCopyCommand.handleEditAction(player, false, args, 0);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1) {
      return Stream.concat(
        IntStream.range(1, SignCopyCommand.NUMBER_OF_LINES + 1)
          .mapToObj(String::valueOf),
        Stream.of(CommandAction.matcher.getNormalizedName(CommandAction.PREVIEW))
      )
        .filter(it -> StringUtils.startsWithIgnoreCase(it, args[0]))
        .toList();
    }

    return List.of();
  }
}

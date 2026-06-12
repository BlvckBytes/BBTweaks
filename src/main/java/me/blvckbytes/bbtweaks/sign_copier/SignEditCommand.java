package me.blvckbytes.bbtweaks.sign_copier;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SignEditCommand implements CommandHandler {

  private final PluginCommand command;
  private final SignCopyCommand signCopyCommand;
  private final ConfigKeeper<MainSection> config;

  public SignEditCommand(
    JavaPlugin plugin,
    SignCopyCommand signCopyCommand,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(SignEditCommandSection.INITIAL_NAME));
    this.signCopyCommand = signCopyCommand;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.signCopier.signEditCommand;
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

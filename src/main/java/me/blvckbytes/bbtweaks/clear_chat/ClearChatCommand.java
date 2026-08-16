package me.blvckbytes.bbtweaks.clear_chat;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class ClearChatCommand implements CommandHandler {

  private final PluginCommand command;

  private final ConfigKeeper<MainSection> config;

  public ClearChatCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(ClearChatCommandSection.INITIAL_NAME));

    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.clearChat.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender)) {
      config.rootSection.clearChat.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.clearChat.actionUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    var flags = EnumSet.noneOf(CommandFlag.class);

    switch (normalizedAction.constant) {
      case SELF -> {
        if (!(sender instanceof Player player)) {
          config.rootSection.clearChat.playersOnly.sendMessage(sender);
          return true;
        }

        if (tryParseFlagsAndGetIfFailed(sender, args, 1, false, flags))
          return true;

        sendBlankLines(player);

        if (!flags.contains(CommandFlag.SILENT))
          config.rootSection.clearChat.clearedChatSelf.sendMessage(sender);
      }

      case OTHER -> {
        if (args.length == 1) {
          config.rootSection.clearChat.otherUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        var targetName = args[1];
        var target = getPlayerByName(targetName);

        if (target == null) {
          config.rootSection.clearChat.targetNotOnline.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("name", targetName)
          );

          return true;
        }

        if (target == sender) {
          config.rootSection.clearChat.otherCannotBeSelf.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        if (tryParseFlagsAndGetIfFailed(sender, args, 2, false, flags))
          return true;

        sendBlankLines(target);

        if (!flags.contains(CommandFlag.SILENT)) {
          config.rootSection.clearChat.clearedChatByOther.sendMessage(
            target,
            new InterpretationEnvironment()
              .withVariable("executor", sender instanceof Player executor ? executor.displayName() : null)
          );
        }

        config.rootSection.clearChat.clearedChatOfTarget.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("silent", flags.contains(CommandFlag.SILENT))
            .withVariable("name", targetName)
        );
      }

      case GLOBAL -> {
        if (tryParseFlagsAndGetIfFailed(sender, args, 1, false, flags))
          return true;

        var environment = new InterpretationEnvironment()
          .withVariable("executor", sender instanceof Player executor ? executor.displayName() : null);

        for (var target : Bukkit.getOnlinePlayers()) {
          sendBlankLines(target);

          if (!flags.contains(CommandFlag.SILENT))
            config.rootSection.clearChat.clearedChatByOtherGlobal.sendMessage(target, environment);
        }

        if (!flags.contains(CommandFlag.SILENT))
          config.rootSection.clearChat.clearedChatByOtherGlobal.sendMessage(Bukkit.getConsoleSender(), environment);
        else
          config.rootSection.clearChat.clearedChatGlobalSilent.sendMessage(sender);
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender) || args.length == 0)
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    var flags = EnumSet.noneOf(CommandFlag.class);

    switch (normalizedAction.constant) {
      case SELF, GLOBAL -> {
        tryParseFlagsAndGetIfFailed(sender, args, 1, true, flags);
        return CommandFlag.createCompletions(args, flags);
      }

      case OTHER -> {
        if (args.length == 2)
          return suggestPlayerNames(args[1], sender instanceof Player player ? player : null);

        tryParseFlagsAndGetIfFailed(sender, args, 2, true, flags);
        return CommandFlag.createCompletions(args, flags);
      }
    }

    return List.of();
  }

  private void sendBlankLines(Player player) {
    var blankLine = Component.text(" ");

    for (var i = 0; i < config.rootSection.clearChat.blankLineCount; ++i)
      player.sendMessage(blankLine);
  }

  private List<String> suggestPlayerNames(String input, @Nullable Player exception) {
    var result = new ArrayList<String>();

    for (var player : Bukkit.getOnlinePlayers()) {
      if (player == exception)
        continue;

      if (StringUtils.startsWithIgnoreCase(player.getName(), input))
        result.add(player.getName());

      var displayName = ComponentUtil.asTrimmedText(player.displayName());

      if (StringUtils.startsWithIgnoreCase(displayName, input))
        result.add(displayName);
    }

    return result;
  }

  private @Nullable Player getPlayerByName(String name) {
    for (var player : Bukkit.getOnlinePlayers()) {
      if (player.getName().equalsIgnoreCase(name))
        return player;

      var displayName = ComponentUtil.asTrimmedText(player.displayName());

      if (displayName.equalsIgnoreCase(name))
        return player;
    }

    return null;
  }

  private boolean tryParseFlagsAndGetIfFailed(CommandSender sender, String[] args, int firstArgIndex, boolean forSuggestions, EnumSet<CommandFlag> output) {
    for (var index = firstArgIndex; index < args.length; ++index) {
      if (forSuggestions && index == args.length - 1)
        break;

      var flagString = args[index];

      if (flagString.isBlank())
        continue;

      var normalizedFlag = CommandFlag.matcher.matchFirst(flagString);

      if (normalizedFlag == null) {
        if (!forSuggestions) {
          config.rootSection.clearChat.unknownFlag.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("unknown_flag", flagString)
              .withVariable("known_flags", CommandFlag.matcher.createCompletions(null))
          );
        }

        return true;
      }

      output.add(normalizedFlag.constant);
    }

    return false;
  }
}

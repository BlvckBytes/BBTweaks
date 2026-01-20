package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class MFilterCommand implements CommandExecutor, TabCompleter {

  private final Command defaultLanguageCommand;
  private final Command customLanguageCommand;
  private final Function<UUID, EditSession> editSessionAccessor;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  public MFilterCommand(
    Command defaultLanguageCommand,
    Command customLanguageCommand,
    Function<UUID, EditSession> editSessionAccessor,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    this.defaultLanguageCommand = defaultLanguageCommand;
    this.customLanguageCommand = customLanguageCommand;
    this.editSessionAccessor = editSessionAccessor;
    this.predicateHelper = predicateHelper;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.mechanic.magnet.filterCommandPlayersOnly.sendMessage(sender);
      return true;
    }

    if (command != defaultLanguageCommand && command != customLanguageCommand)
      return false;

    var editSession = editSessionAccessor.apply(player.getUniqueId());

    if (editSession == null) {
      config.rootSection.mechanic.magnet.filterCommandNoEditSession.sendMessage(sender);
      return true;
    }

    var result = tryParsePredicateAndLanguage(player, label, args, command == defaultLanguageCommand);

    if (result == null)
      return true;

    editSession.setFilter(result);

    config.rootSection.mechanic.magnet.filterCommandFilterSet.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("language", TranslationLanguage.matcher.getNormalizedName(result.language()))
        .withVariable("predicate", new StringifyState(true).appendPredicate(result.predicate()).toString())
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player))
      return null;

    TranslationLanguage language;
    int argsOffset;

    if (command == customLanguageCommand) {
      if (args.length == 1)
        return TranslationLanguage.matcher.createCompletions(args[0]);

      var languageSelection = TranslationLanguage.matcher.matchFirst(args[0]);

      if (languageSelection == null)
        return null;

      language = languageSelection.constant;
      argsOffset = 1;
    }

    else {
      language = predicateHelper.getSelectedLanguage(player);
      argsOffset = 0;
    }

    try {
      var tokens = predicateHelper.parseTokens(args, argsOffset);
      var completions = predicateHelper.createCompletion(language, tokens);

      if (completions.expandedPreviewOrError() != null)
        showActionBarMessage(player, completions.expandedPreviewOrError());

      return completions.suggestions();
    } catch (ItemPredicateParseException e) {
      showActionBarMessage(player, predicateHelper.createExceptionMessage(e));
      return null;
    }
  }

  private @Nullable PredicateAndLanguage tryParsePredicateAndLanguage(Player executor, String label, String[] args, boolean useSelectedLanguage) {
    int predicateArgsOffset;
    TranslationLanguage language;

    if (useSelectedLanguage) {
      language = predicateHelper.getSelectedLanguage(executor);
      predicateArgsOffset = 0;
    }

    else {
      if (args.length == 0) {
        config.rootSection.mechanic.magnet.filterCommandMissingLanguage.sendMessage(
          executor,
          new InterpretationEnvironment()
            .withVariable("label", label)
        );

        return null;
      }

      var languageSelection = TranslationLanguage.matcher.matchFirst(args[0]);

      if (languageSelection == null) {
        config.rootSection.mechanic.magnet.filterCommandUnknownLanguage.sendMessage(
          executor,
          new InterpretationEnvironment()
            .withVariable("input", args[0])
            .withVariable("languages", TranslationLanguage.matcher.createCompletions(null))
        );

        return null;
      }

      language = languageSelection.constant;
      predicateArgsOffset = 1;
    }

    ItemPredicate predicate;

    try {
      var tokens = predicateHelper.parseTokens(args, predicateArgsOffset);
      predicate = predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException e) {
      config.rootSection.mechanic.magnet.filterCommandPredicateError.sendMessage(
        executor,
        new InterpretationEnvironment()
          .withVariable("error", predicateHelper.createExceptionMessage(e))
      );

      return null;
    }

    if (predicate == null) {
      config.rootSection.mechanic.magnet.filterCommandEmptyPredicate.sendMessage(executor);
      return null;
    }

    return new PredicateAndLanguage(predicate, language);
  }

  @SuppressWarnings("deprecation")
  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}

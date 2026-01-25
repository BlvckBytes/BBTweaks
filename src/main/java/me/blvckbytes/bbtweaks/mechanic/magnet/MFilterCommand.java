package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.mechanic.magnet.edit_display.EditDisplayHandler;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.StringifyState;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MFilterCommand implements CommandExecutor, TabCompleter {

  private final Command defaultLanguageCommand;
  private final Command customLanguageCommand;
  private final MagnetMechanic magnetMechanic;
  private final EditDisplayHandler editDisplayHandler;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  public MFilterCommand(
    Command defaultLanguageCommand,
    Command customLanguageCommand,
    MagnetMechanic magnetMechanic,
    EditDisplayHandler editDisplayHandler,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    this.defaultLanguageCommand = defaultLanguageCommand;
    this.customLanguageCommand = customLanguageCommand;
    this.magnetMechanic = magnetMechanic;
    this.editDisplayHandler = editDisplayHandler;
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

    var result = tryParsePredicateAndLanguage(player, label, args, command == defaultLanguageCommand);

    var environment = new InterpretationEnvironment();

    if (result != null) {
      environment
        .withVariable("language", TranslationLanguage.matcher.getNormalizedName(result.language()))
        .withVariable("predicate", new StringifyState(true).appendPredicate(result.predicate()).toString());
    }

    var editSession = magnetMechanic.getEditSessionByPlayer(player);

    if (editSession == null) {
      var sign = getLookedAtSign(player);

      if (sign == null) {
        config.rootSection.mechanic.magnet.filterCommandNoEditSessionAndNoLookedAt.sendMessage(sender);
        return true;
      }

      if (!magnetMechanic.canEditSign(player, sign)) {
        config.rootSection.mechanic.magnet.filterSetByLookingCannotEdit.sendMessage(sender, environment);
        return true;
      }

      if (!PredicateAndLanguage.writeToSignPdcAndGetIfMadeChanges(result, sign, magnetMechanic.filterPredicateKey, magnetMechanic.filterLanguageKey)) {
        if (result == null) {
          config.rootSection.mechanic.magnet.unsetFilterNoneSet.sendMessage(sender, environment);
          return true;
        }

        config.rootSection.mechanic.magnet.filterSetByLookingNoChanges.sendMessage(sender, environment);
        return true;
      }

      sign.update(true, false);
      magnetMechanic.onSignUnload(sign);

      if (sign.getBlock().getState() instanceof Sign newSign)
        magnetMechanic.onSignLoad(newSign);

      if (result == null) {
        config.rootSection.mechanic.magnet.filterSetByLookingUnset.sendMessage(sender, environment);
        return true;
      }

      config.rootSection.mechanic.magnet.filterCommandFilterSet.sendMessage(sender, environment);
      return true;
    }

    if (result == null) {
      config.rootSection.mechanic.magnet.filterCommandEmptyPredicate.sendMessage(player);
      return true;
    }

    editSession.filter = result;

    config.rootSection.mechanic.magnet.filterCommandFilterSet.sendMessage(sender, environment);

    editDisplayHandler.show(player, editSession);

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

  private @Nullable Sign getLookedAtSign(Player player) {
    var rayTraceResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      5.0,
      FluidCollisionMode.NEVER,
      false
    );

    if (rayTraceResult == null || rayTraceResult.getHitBlock() == null)
      return null;

    if (!(rayTraceResult.getHitBlock().getState() instanceof Sign sign))
      return null;

    return sign;
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

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }

  @SuppressWarnings("deprecation")
  private void showActionBarMessage(Player player, String message) {
    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
  }
}

package me.blvckbytes.bbtweaks.multi_break.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakDimension;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.bbtweaks.multi_break.config.MultiBreakLimits;
import me.blvckbytes.bbtweaks.multi_break.display.MultiBreakDisplayData;
import me.blvckbytes.bbtweaks.multi_break.display.MultiBreakDisplayHandler;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MultiBreakCommand implements CommandExecutor, TabCompleter {

  private record SizeValues(int width, int height, int depth) {}

  private final MultiBreakParametersStore parametersStore;
  private final MultiBreakDisplayHandler displayHandler;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  public MultiBreakCommand(
    MultiBreakParametersStore parametersStore,
    MultiBreakDisplayHandler displayHandler,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
    ) {
    this.parametersStore = parametersStore;
    this.displayHandler = displayHandler;
    this.predicateHelper = predicateHelper;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!player.hasPermission("bbtweaks.multibreak")) {
      config.rootSection.multiBreak.missingPermission.sendMessage(player);
      return true;
    }

    if (!config.rootSection.multiBreak.allowedWorlds.contains(player.getWorld().getName())) {
      config.rootSection.multiBreak.unallowedWorld.sendMessage(player);
      return true;
    }

    var parameters = parametersStore.accessParameters(player);

    // Just in case that they've ranked up and now want to increase their extents.
    parameters.updateLimits();

    if (parameters.getLimits() == MultiBreakLimits.ZERO) {
      config.rootSection.multiBreak.noAccessToAnyVolume.sendMessage(player);
      return true;
    }

    if (args.length == 0) {
      // Let's also once again constrain, because it could happen that their limits decreased (temporary permissions, etc.)
      parameters.constrainAndSetFlags(false);

      parameters.clearFlags();

      displayHandler.show(player, new MultiBreakDisplayData(parameters, label));

      config.rootSection.multiBreak.openingSettingsMenu.sendMessage(player);
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      config.rootSection.multiBreak.commandActionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );
      return true;
    }

    if (normalizedAction.constant == CommandAction.SET_FILTER || normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
      var selectedLangauge = predicateHelper.getSelectedLanguage(player);

      int argsOffset;
      TranslationLanguage language;

      if (normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
        NormalizedConstant<TranslationLanguage> matchedLanguage;

        if (args.length == 1 || (matchedLanguage = TranslationLanguage.matcher.matchFirst(args[1])) == null) {
          config.rootSection.multiBreak.commandFilterLanguageUsage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
              .withVariable("languages", TranslationLanguage.matcher.createCompletions(null))
          );
          return true;
        }

        language = matchedLanguage.constant;
        argsOffset = 2;

        if (args.length == argsOffset) {
          config.rootSection.multiBreak.commandFilterUsageCustomLanguage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
              .withVariable("language", TranslationLanguage.matcher.getNormalizedName(language))
          );

          return true;
        }
      }

      else {
        language = selectedLangauge;
        argsOffset = 1;

        if (args.length == argsOffset) {
          config.rootSection.multiBreak.commandFilterUsageDefaultLanguage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }
      }

      ItemPredicate predicate;

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        config.rootSection.multiBreak.predicateError.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("error", predicateHelper.createExceptionMessage(e))
        );

        return true;
      }

      parameters.filter = new PredicateAndLanguage(predicate, language);

      config.rootSection.multiBreak.filterSet.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter_predicate", parameters.filter.getTokenPredicateString())
          .withVariable("set_command", makeFilterSetCommand(label, selectedLangauge, parameters.filter))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case ON -> {
        if (parameters.enabled) {
          config.rootSection.multiBreak.alreadyEnabled.sendMessage(player);
          return true;
        }

        parameters.enabled = true;
        config.rootSection.multiBreak.nowEnabled.sendMessage(player);
        return true;
      }

      case OFF -> {
        if (!parameters.enabled) {
          config.rootSection.multiBreak.alreadyDisabled.sendMessage(player);
          return true;
        }

        parameters.enabled = false;
        config.rootSection.multiBreak.nowDisabled.sendMessage(player);
        return true;
      }

      case SIZE -> {
        SizeValues sizeValues;

        if (args.length != 2 || (sizeValues = tryParseSizeValues(args[1])) == null) {
          config.rootSection.multiBreak.commandSizeUsage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        parameters.setExtent(BreakExtent.LEFT, (sizeValues.width - 1 + 1) / 2, false);
        parameters.setExtent(BreakExtent.RIGHT, (sizeValues.width - 1) / 2, false);
        parameters.setExtent(BreakExtent.UP, (sizeValues.height - 1 + 1) / 2, false);
        parameters.setExtent(BreakExtent.DOWN, (sizeValues.height - 1) / 2, false);
        parameters.setExtent(BreakExtent.DEPTH, sizeValues.depth - 1, false);

        parameters.constrainAndSetFlags(true);

        var exceededDimensions = new HashSet<String>();

        for (var dimension : BreakDimension.values) {
          if (parameters.didExceedLimit(dimension))
            exceededDimensions.add(dimension.name());
        }

        if (!exceededDimensions.isEmpty()) {
          config.rootSection.multiBreak.sizeSetExceededDimensions.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("max_dimension", parameters.getLimits().maxDimension())
              .withVariable("exceeded_dimensions", exceededDimensions)
          );
        }

        parameters.clearFlags();

        var extentLeft = parameters.getExtent(BreakExtent.LEFT);
        var extentRight = parameters.getExtent(BreakExtent.RIGHT);
        var extentUp = parameters.getExtent(BreakExtent.UP);
        var extentDown = parameters.getExtent(BreakExtent.DOWN);
        var extentDepth = parameters.getExtent(BreakExtent.DEPTH);

        config.rootSection.multiBreak.sizeSet.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("total_width", extentLeft + 1 + extentRight)
            .withVariable("total_height", extentUp + 1 + extentDown)
            .withVariable("total_depth", extentDepth + 1)
            .withVariable("extent_left", extentLeft)
            .withVariable("extent_right", extentRight)
            .withVariable("extent_up", extentUp)
            .withVariable("extent_down", extentDown)
            .withVariable("extent_depth", extentDepth)
        );

        return true;
      }

      case GET_FILTER -> {
        if (parameters.filter == null) {
          config.rootSection.multiBreak.noFilterSet.sendMessage(player);
          return true;
        }

        config.rootSection.multiBreak.currentFilter.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("filter_predicate", parameters.filter.getTokenPredicateString())
            .withVariable("set_command", makeFilterSetCommand(label, predicateHelper.getSelectedLanguage(player), parameters.filter))
        );
        return true;
      }

      case REMOVE_FILTER -> {
        if (parameters.filter == null) {
          config.rootSection.multiBreak.noFilterSet.sendMessage(player);
          return true;
        }

        config.rootSection.multiBreak.filterRemoved.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("filter_predicate", parameters.filter.getTokenPredicateString())
            .withVariable("set_command", makeFilterSetCommand(label, predicateHelper.getSelectedLanguage(player), parameters.filter))
        );

        parameters.filter = null;
        return true;
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !player.hasPermission("bbtweaks.multibreak"))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (normalizedAction.constant == CommandAction.SET_FILTER || normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE)
      return PredicateUtils.tabCompletePredicate(player, args, 1, predicateHelper, normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE);

    if (normalizedAction.constant == CommandAction.SIZE) {
      var parameters = parametersStore.accessParameters(player);

      // Seeing how seldomly this branch is invoked, I'd rather keep the displayed suggestions in sync with the current permissions.
      parameters.updateLimits();

      var maxDimension = parameters.getLimits().maxDimension();

      var suggestedSizes = new ArrayList<String>();

      for (var size = 2; size <= 5; ++size) {
        if (size > maxDimension)
          break;

        suggestedSizes.add(size + "x" + size + "x" + size);
      }

      return suggestedSizes;
    }

    return List.of();
  }

  private String makeFilterSetCommand(String label, TranslationLanguage currentLanguage, PredicateAndLanguage predicateAndLanguage) {
    if (currentLanguage == predicateAndLanguage.language)
      return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER) + " " + predicateAndLanguage.getTokenPredicateString();

    return "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER_WITH_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(predicateAndLanguage.language) + " " + predicateAndLanguage.getTokenPredicateString();
  }

  private @Nullable SizeValues tryParseSizeValues(String input) {
    var sizeParts = input.split("x");

    if (sizeParts.length != 3)
      return null;

    int width, height, depth;

    try {
      width = Integer.parseInt(sizeParts[0]);
      height = Integer.parseInt(sizeParts[1]);
      depth = Integer.parseInt(sizeParts[2]);
    } catch (Throwable e) {
      return null;
    }

    if (width < 0 || height < 0 || depth < 0)
      return null;

    return new SizeValues(width, height, depth);
  }
}

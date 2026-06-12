package me.blvckbytes.bbtweaks.multi_break.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.multi_break.config.MultiBreakCommandSection;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakDimension;
import me.blvckbytes.bbtweaks.multi_break.parameters.BreakExtent;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParameters;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.bbtweaks.multi_break.display.MultiBreakDisplayData;
import me.blvckbytes.bbtweaks.multi_break.display.MultiBreakDisplayHandler;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class MultiBreakCommand implements CommandHandler {

  private record SizeValues(int width, int height, int depth) {}

  private final PluginCommand command;
  private final MultiBreakParametersStore parametersStore;
  private final MultiBreakDisplayHandler displayHandler;
  private final PredicateHelper predicateHelper;
  private final ConfigKeeper<MainSection> config;

  public MultiBreakCommand(
    JavaPlugin plugin,
    MultiBreakParametersStore parametersStore,
    MultiBreakDisplayHandler displayHandler,
    PredicateHelper predicateHelper,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(MultiBreakCommandSection.INITIAL_NAME));
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

    var parametersSlots = parametersStore.accessParametersSlots(player);

    // Just in case that they've ranked up and now want to increase their extents.
    parametersSlots.updateLimits();

    // Let's also once again constrain, because it could happen that their limits decreased (temporary permissions, etc.)
    parametersSlots.parametersBySlotIndex.forEach(parameters -> parameters.constrainAndSetFlags(false));

    if (parametersSlots.getLimits().maxDimension() == 0) {
      parametersSlots.parametersBySlotIndex.forEach(MultiBreakParameters::zeroOutAllExtents);
      parametersSlots.enabled = false;

      config.rootSection.multiBreak.noAccessToAnyVolume.sendMessage(player);
      return true;
    }

    if (args.length == 0) {
      displayHandler.show(player, new MultiBreakDisplayData(parametersSlots, label));
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

    var selectedParameters = parametersSlots.getSelectedParameters();

    if (normalizedAction.constant == CommandAction.SET_FILTER || normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
      if (selectedParameters.tellIfLocked())
        return true;

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

      selectedParameters.filter = new PredicateAndLanguage(predicate, language);
      selectedParameters.filterEnabled = true;

      config.rootSection.multiBreak.filterSet.sendMessage(
        player,
          selectedParameters.makeEnvironment()
          .withVariable("set_command", selectedParameters.makeFilterSetCommand(label, selectedLangauge))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case ON -> {
        parametersSlots.setEnabled(true);
        return true;
      }

      case OFF -> {
        parametersSlots.setEnabled(false);
        return true;
      }

      case TOGGLE -> {
        parametersSlots.setEnabled(null);
        return true;
      }

      case SIZE -> {
        if (selectedParameters.tellIfLocked())
          return true;

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

        selectedParameters.setExtent(BreakExtent.LEFT, (sizeValues.width - 1 + 1) / 2, false);
        selectedParameters.setExtent(BreakExtent.RIGHT, (sizeValues.width - 1) / 2, false);
        selectedParameters.setExtent(BreakExtent.UP, (sizeValues.height - 1 + 1) / 2, false);
        selectedParameters.setExtent(BreakExtent.DOWN, (sizeValues.height - 1) / 2, false);
        selectedParameters.setExtent(BreakExtent.DEPTH, sizeValues.depth - 1, false);

        selectedParameters.constrainAndSetFlags(true);

        var exceededDimensions = new HashSet<String>();

        for (var dimension : BreakDimension.values) {
          if (selectedParameters.didExceedLimit(dimension))
            exceededDimensions.add(dimension.name());
        }

        if (!exceededDimensions.isEmpty()) {
          config.rootSection.multiBreak.sizeSetExceededDimensions.sendMessage(
            player,
            selectedParameters.makeEnvironment()
              .withVariable("exceeded_dimensions", exceededDimensions)
          );
        }

        selectedParameters.clearFlags();

        config.rootSection.multiBreak.sizeSet.sendMessage(player, selectedParameters.makeEnvironment());
        return true;
      }

      case GET_FILTER -> {
        var setCommand = selectedParameters.makeFilterSetCommand(label, predicateHelper.getSelectedLanguage(player));

        if (setCommand == null) {
          config.rootSection.multiBreak.noFilterSet.sendMessage(player, selectedParameters.makeEnvironment());
          return true;
        }

        config.rootSection.multiBreak.currentFilter.sendMessage(
          player,
          selectedParameters.makeEnvironment()
            .withVariable("set_command", setCommand)
        );

        return true;
      }

      case REMOVE_FILTER -> {
        selectedParameters.removeFilter(label, predicateHelper.getSelectedLanguage(player));
        return true;
      }

      case ENABLE_FILTER -> {
        selectedParameters.setFilterEnabled(true);
        return true;
      }

      case DISABLE_FILTER -> {
        selectedParameters.setFilterEnabled(false);
        return true;
      }

      case TOGGLE_FILTER -> {
        selectedParameters.setFilterEnabled(null);
        return true;
      }

      case SELECT_SLOT -> {
        int slot;

        try {
          if (args.length != 2)
            throw new IllegalStateException();

          slot = Integer.parseInt(args[1]);

          if (slot <= 0 || slot > parametersSlots.parametersBySlotIndex.size())
            throw new IllegalStateException();
        } catch (Throwable e) {
          config.rootSection.multiBreak.selectSlotUsage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
              .withVariable("max_slot", parametersSlots.parametersBySlotIndex.size())
          );

          return true;
        }

        parametersSlots.setSelectedSlotIndex(slot - 1, true);
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

    var parametersSlots = parametersStore.accessParametersSlots(player);

    if (normalizedAction.constant == CommandAction.SIZE) {
      // Seeing how seldomly this branch is invoked, I'd rather keep the displayed suggestions in sync with the current permissions.
      parametersSlots.updateLimits();

      var maxDimension = parametersSlots.getLimits().maxDimension();

      var suggestedSizes = new ArrayList<String>();

      for (var size = 2; size <= 5; ++size) {
        if (size > maxDimension)
          break;

        suggestedSizes.add(size + "x" + size + "x" + size);
      }

      return suggestedSizes;
    }

    if (normalizedAction.constant == CommandAction.SELECT_SLOT) {
      if (args.length != 2)
        return List.of();

      return IntStream.range(1, parametersSlots.parametersBySlotIndex.size() + 1)
        .mapToObj(String::valueOf)
        .filter(it -> it.startsWith(args[1]))
        .toList();
    }

    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.multiBreak.command;
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

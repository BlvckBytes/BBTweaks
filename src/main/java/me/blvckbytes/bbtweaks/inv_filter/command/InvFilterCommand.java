package me.blvckbytes.bbtweaks.inv_filter.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterProfile;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterProfileStore;
import me.blvckbytes.bbtweaks.inv_filter.display.InvFilterDisplayData;
import me.blvckbytes.bbtweaks.inv_filter.display.InvFilterDisplayHandler;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

public class InvFilterCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final IPPIntegration ippIntegration;
  private final InvFilterProfileStore profileStore;
  private final InvFilterDisplayHandler displayHandler;
  private final ConfigKeeper<MainSection> config;

  public InvFilterCommand(
    JavaPlugin plugin,
    IPPIntegration ippIntegration,
    InvFilterProfileStore profileStore,
    InvFilterDisplayHandler displayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(InvFilterCommandSection.INITIAL_NAME));

    this.ippIntegration = ippIntegration;
    this.profileStore = profileStore;
    this.displayHandler = displayHandler;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.invFilter.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.invfilter")) {
      config.rootSection.invFilter.noPermission.sendMessage(player);
      return true;
    }

    var profile = profileStore.access(player);

    if (args.length == 0) {
      displayHandler.show(player, new InvFilterDisplayData(profile, label));
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      config.rootSection.invFilter.usageAction.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case OFF -> profile.setEnabledAndMessage(label, false);
      case ON -> profile.setEnabledAndMessage(label, true);
      case TOGGLE -> profile.setEnabledAndMessage(label, null);
      case SELECT_SLOT -> {
        if (args.length != 2) {
          config.rootSection.invFilter.usageSelectSlot.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        var slot = tryParseSlotNumberOrMessage(player, args[1], profile);

        if (slot == null)
          return true;

        profile.setSelectedSlotIndexAndMessage(label, slot - 1);

        if (!profile.isEnabled())
          profile.setEnabledAndMessage(label, true);
      }

      case GET_FILTER -> {
        int slotIndex;

        if (args.length == 1) {
          slotIndex = profile.getSelectedSlotIndex();
        } else if (args.length == 2) {
          var slot = tryParseSlotNumberOrMessage(player, args[1], profile);

          if (slot == null)
            return true;

          slotIndex = slot - 1;
        } else {
          config.rootSection.invFilter.usageGetFilter.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        var currentFilter = profile.getFilter(slotIndex);

        if (currentFilter == null) {
          config.rootSection.invFilter.getFilterNoneSet.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("slot", slotIndex + 1)
          );

          return true;
        }

        config.rootSection.invFilter.getFilter.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("slot", slotIndex + 1)
            .withVariable("filter", currentFilter.getTokenPredicateString())
            .withVariable("set_filter_command", profile.makeSetFilterCommand(label, currentFilter))
        );
      }

      case REMOVE_FILTER -> {
        profile.removeCurrentFilterIfSetAndMessage(label);
      }

      case SET_FILTER, SET_FILTER_WITH_LANGUAGE -> {
        int argsOffset;
        TranslationLanguage language;

        if (normalizedAction.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
          me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant<TranslationLanguage> matchedLanguage;

          if (args.length == 1 || (matchedLanguage = TranslationLanguage.matcher.matchFirst(args[1])) == null) {
            config.rootSection.invFilter.usageLanguage.sendMessage(
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
            config.rootSection.invFilter.usageFilterCustomLanguage.sendMessage(
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
          language = ippIntegration.predicateHelper.getSelectedLanguage(player);
          argsOffset = 1;

          if (args.length == argsOffset) {
            config.rootSection.invFilter.usageFilterDefaultLanguage.sendMessage(
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
          var tokens = ippIntegration.predicateHelper.parseTokens(args, argsOffset);
          predicate = ippIntegration.predicateHelper.parsePredicate(language, tokens);
        } catch (ItemPredicateParseException e) {
          config.rootSection.invFilter.predicateError.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("error", ippIntegration.predicateHelper.createExceptionMessage(e))
          );

          return true;
        }

        var newFilter = new PredicateAndLanguage(predicate, language);

        profile.setFilterToCurrentSlot(newFilter);

        config.rootSection.invFilter.filterSet.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("slot", profile.getSelectedSlotIndex() + 1)
            .withVariable("filter", newFilter.getTokenPredicateString())
            .withVariable("set_filter_command", profile.makeSetFilterCommand(label, newFilter))
        );

        if (!profile.isEnabled())
          profile.setEnabledAndMessage(label, true);
      }

      default -> throw new IllegalStateException("Unaccounted-for command-action: " + normalizedAction.constant.name());
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (!player.hasPermission("bbtweaks.invfilter"))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var action = CommandAction.matcher.matchFirst(args[0]);

    if (action == null)
      return List.of();

    if (action.constant == CommandAction.SET_FILTER || action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE)
      return PredicateUtils.tabCompletePredicate(player, args, 1, ippIntegration, action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE);

    if (action.constant == CommandAction.SELECT_SLOT || action.constant == CommandAction.GET_FILTER) {
      var profile = profileStore.access(player);

      return IntStream.range(1, profile.getSlotCount() + 1)
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
    return config.rootSection.invFilter.command;
  }

  private @Nullable Integer tryParseSlotNumberOrMessage(Player player, String input, InvFilterProfile profile) {
    try {
      var slot = Integer.parseInt(input);

      if (slot <= 0 || slot > profile.getSlotCount())
        throw new IllegalArgumentException();

      return slot;
    } catch (Throwable _) {
      config.rootSection.invFilter.invalidFilterSlot.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", input)
          .withVariable("slot_count", profile.getSlotCount())
      );

      return null;
    }
  }
}

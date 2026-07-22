package me.blvckbytes.bbtweaks.pipes.search.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.EnumerationBehavior;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeEnumerationSessionHandler;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeSearchSession;
import me.blvckbytes.bbtweaks.pipes.predicates.PipeBlockUtility;
import me.blvckbytes.bbtweaks.pipes.search.ItemAndSlot;
import me.blvckbytes.bbtweaks.pipes.search.ItemCollectionEntry;
import me.blvckbytes.bbtweaks.pipes.search.display.SearchDisplayData;
import me.blvckbytes.bbtweaks.pipes.search.display.PipeSearchDisplayHandler;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeSearchCommand implements CommandHandler {

  private final PluginCommand command;

  private final Plugin plugin;
  private final PipeEnumerationSessionHandler enumerationSessionHandler;
  private final PipeSearchDisplayHandler pipeSearchDisplayHandler;
  private final IPPIntegration ippIntegration;
  private final ConfigKeeper<MainSection> config;

  public PipeSearchCommand(
    JavaPlugin plugin,
    PipeEnumerationSessionHandler enumerationSessionHandler,
    PipeSearchDisplayHandler pipeSearchDisplayHandler,
    IPPIntegration ippIntegration,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(PipeSearchCommandSection.INITIAL_NAME));

    this.plugin = plugin;
    this.enumerationSessionHandler = enumerationSessionHandler;
    this.pipeSearchDisplayHandler = pipeSearchDisplayHandler;
    this.ippIntegration = ippIntegration;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.pipes.search.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.pipes.search.command.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.pipes.search.command.noPermission.sendMessage(sender);
      return true;
    }

    var remainingArgs = new ArrayList<>(Arrays.asList(args));

    EnumSet<CommandFlag> flags;

    try {
      flags = CommandFlag.consumeLeadingFlags(remainingArgs);
    } catch (UnknownCommandFlagException e) {
      config.rootSection.pipes.search.command.unknownFlag.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("unknown_flag", e.flagValue)
          .withVariable("known_flags", CommandFlag.matcher.createCompletions(null))
      );

      return true;
    }

    PredicateAndLanguage predicateAndLanguage = null;

    if (!remainingArgs.isEmpty()) {
      predicateAndLanguage = tryParsePredicate(player, remainingArgs.toArray(new String[0]));

      if (predicateAndLanguage == null)
        return true;
    }

    var predicate = predicateAndLanguage == null ? null : predicateAndLanguage.predicate;

    var targetBlock = PipeBlockUtility.resolveFacedTargetBlock(player);

    PipeSearchSession.tryStartSessionOrNotify(
      enumerationSessionHandler, player, targetBlock,
      flags.contains(CommandFlag.IGNORE_NON_STORAGE),
      (
        flags.contains(CommandFlag.HONOR_CHECK_VALVES)
          ? EnumSet.noneOf(EnumerationBehavior.class)
          : EnumSet.of(EnumerationBehavior.IGNORE_CHECK_VALVES)
      ),
      searchSession -> handleSearchCompletion(searchSession, player, predicate)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (!command.testPermission(player) || args.length == 0)
      return List.of();

    var lastArg = args[args.length - 1];

    if (lastArg.startsWith("-")) {
      for (var priorIndex = args.length - 2; priorIndex >= 0; --priorIndex) {
        if (!args[priorIndex].startsWith("-"))
          return List.of();
      }

      return CommandFlag.matcher.createCompletions(lastArg.substring(1)).stream()
        .map(flag -> "-" + flag)
        .toList();
    }

    return PredicateUtils.tabCompletePredicate(player, args, 0, ippIntegration, false);
  }

  private void handleSearchCompletion(PipeSearchSession session, Player player, @Nullable ItemPredicate predicate) {
    if (session.getSearchedInventories().isEmpty()) {
      config.rootSection.pipes.search.searchNoContainers.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("piston_count", session.getPistonCount())
          .withVariable("tube_count", session.getTubeCount())
      );

      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      var matches = new ArrayList<ItemAndSlot>();

      var resultCounter = 0;

      for (var searchedInventory : session.getSearchedInventories()) {
        var blockContents = searchedInventory.inventory().getStorageContents();

        for (var slotIndex = 0; slotIndex < blockContents.length; ++slotIndex) {
          var item = blockContents[slotIndex];

          if (item == null || item.getType().isAir())
            continue;

          ++resultCounter;

          if (predicate != null && !predicate.test(item))
            continue;

          matches.add(new ItemAndSlot(item, searchedInventory.block(), slotIndex + searchedInventory.slotOffset()));
        }
      }

      var containerCounts = new ArrayList<ContainerCount>();
      var totalContainerCount = session.forEachContainerCountAndGetSum((material, amount) -> containerCounts.add(new ContainerCount(material, amount)));

      var environment = new InterpretationEnvironment()
        .withVariable("predicate", predicate == null ? "/" : PlainStringifier.stringify(predicate, true))
        .withVariable("item_count", resultCounter)
        .withVariable("total_container_count", totalContainerCount)
        .withVariable("container_counts", containerCounts)
        .withVariable("piston_count", session.getPistonCount())
        .withVariable("tube_count", session.getTubeCount())
        .withVariable("match_count", matches.size());

      if (matches.isEmpty()) {
        config.rootSection.pipes.search.searchNoResults.sendMessage(player, environment);
        return;
      }

      config.rootSection.pipes.search.searchShowingResults.sendMessage(player, environment);

      // Let's show the bucketed overview by default instead of the other way around, as I
      // believe that there's not much of a need for the individual screen anymore.
      var displayData = ItemCollectionEntry.collectEntries(matches);

      pipeSearchDisplayHandler.show(player, new SearchDisplayData(predicate, displayData, null));
    });
  }

  private @Nullable PredicateAndLanguage tryParsePredicate(Player executor, String[] args) {
    var language = ippIntegration.predicateHelper.getSelectedLanguage(executor);

    ItemPredicate predicate;

    try {
      var tokens = ippIntegration.predicateHelper.parseTokens(args, 0);
      predicate = ippIntegration.predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException e) {
      config.rootSection.pipes.search.predicateError.sendMessage(
        executor,
        new InterpretationEnvironment()
          .withVariable("error_message", ippIntegration.predicateHelper.createExceptionMessage(e))
      );

      return null;
    }

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }
}

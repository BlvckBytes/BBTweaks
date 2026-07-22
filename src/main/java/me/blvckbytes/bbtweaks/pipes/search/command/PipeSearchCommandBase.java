package me.blvckbytes.bbtweaks.pipes.search.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.EnumerationBehavior;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeEnumerationSessionHandler;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeSearchSession;
import me.blvckbytes.bbtweaks.pipes.predicates.PipeBlockUtility;
import me.blvckbytes.bbtweaks.pipes.search.ItemAndSlot;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class PipeSearchCommandBase<ParameterType extends PipeSearchParameter> implements CommandHandler {

  protected final Plugin plugin;
  protected final IPPIntegration ippIntegration;
  protected final ConfigKeeper<MainSection> config;
  private final PipeEnumerationSessionHandler enumerationSessionHandler;

  public PipeSearchCommandBase(
    JavaPlugin plugin,
    IPPIntegration ippIntegration,
    ConfigKeeper<MainSection> config,
    PipeEnumerationSessionHandler enumerationSessionHandler
  ) {
    this.plugin = plugin;
    this.ippIntegration = ippIntegration;
    this.config = config;
    this.enumerationSessionHandler = enumerationSessionHandler;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.pipes.search.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.pipes.search.noPermission.sendMessage(sender);
      return true;
    }

    var remainingArgs = new ArrayList<>(Arrays.asList(args));

    EnumSet<CommandFlag> flags;

    try {
      flags = CommandFlag.consumeLeadingFlags(remainingArgs);
    } catch (UnknownCommandFlagException e) {
      config.rootSection.pipes.search.unknownFlag.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("unknown_flag", e.flagValue)
          .withVariable("known_flags", CommandFlag.matcher.createCompletions(null))
      );

      return true;
    }

    handleCommand(player, flags, remainingArgs.toArray(String[]::new));
    return true;
  }

  protected abstract void handleCommand(Player player, EnumSet<CommandFlag> flags, String[] remainingArgs);

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

    var remainingArgs = new ArrayList<>(Arrays.asList(args));

    try {
      CommandFlag.consumeLeadingFlags(remainingArgs);
    } catch (UnknownCommandFlagException _) {
      return List.of();
    }

    return handleTabComplete(player, remainingArgs.toArray(String[]::new));
  }

  protected abstract List<String> handleTabComplete(Player player, String[] remainingArgs);

  protected void startSearch(Player player, ParameterType parameter) {
    var targetBlock = PipeBlockUtility.resolveFacedTargetBlock(player);

    PipeSearchSession.tryStartSessionOrNotify(
      enumerationSessionHandler, player, targetBlock,
      parameter.flags.contains(CommandFlag.IGNORE_NON_STORAGE),
      (
        parameter.flags.contains(CommandFlag.HONOR_CHECK_VALVES)
          ? EnumSet.noneOf(EnumerationBehavior.class)
          : EnumSet.of(EnumerationBehavior.IGNORE_CHECK_VALVES)
      ),
      searchSession -> handleSearchCompletion(searchSession, player, parameter)
    );
  }

  protected abstract void handleMatchingItemsAsync(
    PipeSearchSession session,
    Player player,
    ParameterType parameter,
    List<ItemAndSlot> matches,
    InterpretationEnvironment environment
  );

  private void handleSearchCompletion(PipeSearchSession session, Player player, ParameterType parameter) {
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

      var totalItemCount = 0;

      for (var searchedInventory : session.getSearchedInventories()) {
        var blockContents = searchedInventory.inventory().getStorageContents();

        for (var slotIndex = 0; slotIndex < blockContents.length; ++slotIndex) {
          var item = blockContents[slotIndex];

          if (item == null || item.getType().isAir())
            continue;

          ++totalItemCount;

          if (parameter.predicateAndLanguage != null && !parameter.predicateAndLanguage.predicate.test(item))
            continue;

          matches.add(new ItemAndSlot(item, searchedInventory.block(), slotIndex + searchedInventory.slotOffset()));
        }
      }

      var containerCounts = new ArrayList<ContainerCount>();
      var totalContainerCount = session.forEachContainerCountAndGetSum((material, amount) -> containerCounts.add(new ContainerCount(material, amount)));

      var environment = new InterpretationEnvironment()
        .withVariable("predicate", parameter.predicateAndLanguage == null ? "/" : parameter.predicateAndLanguage.getTokenPredicateString())
        .withVariable("item_count", totalItemCount)
        .withVariable("total_container_count", totalContainerCount)
        .withVariable("container_counts", containerCounts)
        .withVariable("piston_count", session.getPistonCount())
        .withVariable("tube_count", session.getTubeCount())
        .withVariable("match_count", matches.size());

      if (matches.isEmpty()) {
        config.rootSection.pipes.search.searchNoResults.sendMessage(player, environment);
        return;
      }

      handleMatchingItemsAsync(session, player, parameter, matches, environment);
    });
  }

  protected @Nullable PredicateAndLanguage tryParsePredicate(Player executor, String[] args) {
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

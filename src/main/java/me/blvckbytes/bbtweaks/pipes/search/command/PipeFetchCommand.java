package me.blvckbytes.bbtweaks.pipes.search.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.ast.ExpressionNode;
import at.blvckbytes.component_markup.expression.interpreter.ExpressionInterpreter;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.expression.parser.ExpressionParseException;
import at.blvckbytes.component_markup.expression.parser.ExpressionParser;
import at.blvckbytes.component_markup.expression.tokenizer.ExpressionTokenizeException;
import at.blvckbytes.component_markup.util.InputView;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeEnumerationSessionHandler;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeSearchSession;
import me.blvckbytes.bbtweaks.pipes.search.ItemAndSlot;
import me.blvckbytes.bbtweaks.pipes.search.ItemCollectionEntry;
import me.blvckbytes.bbtweaks.pipes.search.display.PipeSearchDisplayHandler;
import me.blvckbytes.bbtweaks.util.MutableInt;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class PipeFetchCommand extends PipeSearchCommandBase<PipeFetchParameter> {

  private static final String ALL_SENTINEL = "all";

  private final PluginCommand command;

  private final PipeSearchDisplayHandler pipeSearchDisplayHandler;

  public PipeFetchCommand(
    JavaPlugin plugin,
    IPPIntegration ippIntegration,
    ConfigKeeper<MainSection> config,
    PipeEnumerationSessionHandler enumerationSessionHandler,
    PipeSearchDisplayHandler pipeSearchDisplayHandler
  ) {
    super(plugin, ippIntegration, config, enumerationSessionHandler);

    this.command = Objects.requireNonNull(plugin.getCommand(PipeFetchCommandSection.INITIAL_NAME));

    this.pipeSearchDisplayHandler = pipeSearchDisplayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.pipes.search.fetchCommand;
  }

  @Override
  protected void handleCommand(Player player, EnumSet<CommandFlag> flags, String label, String[] remainingArgs) {
    NormalizedConstant<FetchMode> normalizedMode;

    if (remainingArgs.length < 3 || (normalizedMode = FetchMode.matcher.matchFirst(remainingArgs[0])) == null) {
      player.sendMessage("§cMode-Usage");
      config.rootSection.pipes.search.fetchCommand.usage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("modes", FetchMode.matcher.createCompletions(null))
      );

      return;
    }

    var amountString = remainingArgs[1];
    Long maximumAmount = 0L;

    if (!amountString.equalsIgnoreCase(ALL_SENTINEL)) {
      maximumAmount = parseNumericExpression(amountString);

      if (maximumAmount == null) {
        config.rootSection.pipes.search.fetchCommand.malformedMaximumAmount.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("input", amountString)
        );

        return;
      }

      if (maximumAmount <= 0) {
        config.rootSection.pipes.search.fetchCommand.nonPositiveMaximumAmount.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("amount", maximumAmount)
        );

        return;
      }
    }

    var predicateAndLanguage = tryParsePredicate(player, remainingArgs, 2);

    if (predicateAndLanguage == null)
      return;

    startSearch(player, new PipeFetchParameter(predicateAndLanguage, flags, normalizedMode.constant, maximumAmount.intValue()));
  }

  @Override
  protected List<String> handleTabComplete(Player player, String[] remainingArgs) {
    if (remainingArgs.length == 1)
      return FetchMode.matcher.createCompletions(remainingArgs[0]);

    if (remainingArgs.length == 2) {
      return Stream.of("1", "16", "32", "64", "4*64", ALL_SENTINEL)
        .filter(it -> it.startsWith(remainingArgs[1]))
        .toList();
    }

    return PredicateUtils.tabCompletePredicate(player, remainingArgs, 2, ippIntegration, false);
  }

  @Override
  protected void handleMatchingItemsAsync(
    PipeSearchSession session,
    Player player,
    PipeFetchParameter parameter,
    List<ItemAndSlot> matches,
    InterpretationEnvironment environment
  ) {
    var collections = ItemCollectionEntry.collectEntries(matches);

    Bukkit.getScheduler().runTask(plugin, () -> {
      var maximumAmount = parameter.maximumAmount <= 0 ? Integer.MAX_VALUE : parameter.maximumAmount;

      for (var collection : collections) {
        pipeSearchDisplayHandler.handleMovingItems(player, null, collection, maximumAmount);

        if (parameter.fetchMode == FetchMode.FIRST)
          break;
      }
    });
  }

  private @Nullable Long parseNumericExpression(String expression) {
    var inputView = InputView.of(expression);

    ExpressionNode expressionNode;

    try {
      expressionNode = ExpressionParser.parse(inputView, null);
    } catch (ExpressionTokenizeException | ExpressionParseException e) {
      return null;
    }

    var environment = new InterpretationEnvironment();

    var logCount = new MutableInt();

    var value = ExpressionInterpreter.interpret(expressionNode, environment, (_, _, _, _) -> ++logCount.value);

    if (logCount.value != 0)
      return null;

    return environment.getValueInterpreter().asLong(value);
  }
}

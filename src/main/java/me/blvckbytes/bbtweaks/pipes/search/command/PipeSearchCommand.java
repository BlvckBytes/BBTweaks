package me.blvckbytes.bbtweaks.pipes.search.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeEnumerationSessionHandler;
import me.blvckbytes.bbtweaks.pipes.enumeration_session.PipeSearchSession;
import me.blvckbytes.bbtweaks.pipes.search.ItemAndSlot;
import me.blvckbytes.bbtweaks.pipes.search.ItemCollectionEntry;
import me.blvckbytes.bbtweaks.pipes.search.display.SearchDisplayData;
import me.blvckbytes.bbtweaks.pipes.search.display.PipeSearchDisplayHandler;
import me.blvckbytes.bbtweaks.util.PredicateUtils;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipeSearchCommand extends PipeSearchCommandBase<PipeSearchParameter> {

  private final PluginCommand command;

  private final PipeSearchDisplayHandler pipeSearchDisplayHandler;

  public PipeSearchCommand(
    JavaPlugin plugin,
    PipeEnumerationSessionHandler enumerationSessionHandler,
    PipeSearchDisplayHandler pipeSearchDisplayHandler,
    IPPIntegration ippIntegration,
    ConfigKeeper<MainSection> config
  ) {
    super(plugin, ippIntegration, config, enumerationSessionHandler);

    this.command = Objects.requireNonNull(plugin.getCommand(PipeSearchCommandSection.INITIAL_NAME));

    this.pipeSearchDisplayHandler = pipeSearchDisplayHandler;
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
  protected void handleCommand(Player player, EnumSet<CommandFlag> flags, String label, String[] remainingArgs) {
    PredicateAndLanguage predicateAndLanguage = null;

    if (remainingArgs.length > 0) {
      predicateAndLanguage = tryParsePredicate(player, remainingArgs, 0);

      if (predicateAndLanguage == null)
        return;
    }

    startSearch(player, new PipeSearchParameter(predicateAndLanguage, flags));
  }

  @Override
  protected List<String> handleTabComplete(Player player, String[] remainingArgs) {
    return PredicateUtils.tabCompletePredicate(player, remainingArgs, 0, ippIntegration, false);
  }

  @Override
  protected void handleMatchingItemsAsync(
    PipeSearchSession session,
    Player player,
    PipeSearchParameter parameter,
    List<ItemAndSlot> matches,
    InterpretationEnvironment environment
  ) {
    config.rootSection.pipes.search.searchShowingResults.sendMessage(player, environment);

    // Let's show the bucketed overview by default instead of the other way around, as I
    // believe that there's not much of a need for the individual screen anymore.
    var displayData = ItemCollectionEntry.collectEntries(matches);

    pipeSearchDisplayHandler.show(player, new SearchDisplayData(parameter.predicateAndLanguage, displayData, null));
  }
}

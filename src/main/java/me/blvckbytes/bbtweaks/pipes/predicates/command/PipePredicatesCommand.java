package me.blvckbytes.bbtweaks.pipes.predicates.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.bbtweaks.pipes.predicates.PipeBlockUtility;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicateEventHandler;
import me.blvckbytes.bbtweaks.pipes.search.command.PipeSearchCommand;
import me.blvckbytes.item_predicate_parser.event.PredicateAndLanguage;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.keyed.DisjunctionKey;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PipePredicatesCommand implements CommandHandler, Listener {

  private final PluginCommand command;
  private final IPPIntegration ippIntegration;
  private final PipeSearchCommand pipeSearchCommand;
  private final PipePredicateEventHandler pipePredicateEventHandler;
  private final ConfigKeeper<MainSection> config;

  public PipePredicatesCommand(
    JavaPlugin plugin,
    IPPIntegration ippIntegration,
    PipeSearchCommand pipeSearchCommand,
    PipePredicateEventHandler pipePredicateEventHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(PipePredicatesCommandSection.INITIAL_NAME));

    this.ippIntegration = ippIntegration;
    this.pipeSearchCommand = pipeSearchCommand;
    this.pipePredicateEventHandler = pipePredicateEventHandler;
    this.config = config;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.pipes.predicates.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.pipes.predicates.command.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.pipes.predicates.command.noPermission.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.pipes.predicates.command.actionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    // Keep these aliases around, as they're already broadly known on our server.
    switch (normalizedAction.constant) {
      case SET, SET_LANGUAGE, GET, REMOVE:
        return ippIntegration.mainCommand.getExecutor().onCommand(sender, command, label, args);
    }

    // Keep this alias around, as it's already broadly known on our server.
    if (normalizedAction.constant == CommandAction.SEARCH)
      return pipeSearchCommand.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));

    if (normalizedAction.constant == CommandAction.GENERATE) {
      var pistonBlock = PipeBlockUtility.resolvePistonBlock(PipeBlockUtility.resolveFacedTargetBlock(player));

      if (pistonBlock == null || !(pistonBlock.getBlockData() instanceof Directional directional)) {
        config.rootSection.pipes.predicates.command.generateNotLookingAtPipeBlock.sendMessage(player);
        return true;
      }

      if (!(pistonBlock.getRelative(directional.getFacing()).getState() instanceof Container container)) {
        config.rootSection.pipes.predicates.command.generateNoContainer.sendMessage(player);
        return true;
      }

      var allowInitialize = player.hasPermission("bbtweaks.pipes.auto-init-signs");
      var pistonSign = PipeBlockUtility.getPistonSign(pistonBlock, allowInitialize);

      if (pistonSign == null) {
        config.rootSection.pipes.predicates.command.generateNoSign.sendMessage(player);
        return true;
      }

      if (!pipePredicateEventHandler.canEditSign(player, pistonSign)) {
        config.rootSection.pipes.predicates.command.generateCannotEditSign.sendMessage(player);
        return true;
      }

      var targetLanguage = ippIntegration.predicateHelper.getSelectedLanguage(player);
      var translationRegistry = ippIntegration.languageRegistry.getTranslationRegistry(targetLanguage);

      var containedMaterials = new HashSet<Material>();

      for (var storedItem : container.getInventory().getStorageContents()) {
        if (storedItem != null && !storedItem.getType().isAir())
          containedMaterials.add(storedItem.getType());
      }

      if (containedMaterials.isEmpty()) {
        config.rootSection.pipes.predicates.command.generateEmptyContainer.sendMessage(player);
        return true;
      }

      var sortedMaterials = new ArrayList<>(containedMaterials);
      sortedMaterials.sort(Comparator.comparingInt(Enum::ordinal));

      var orTranslation = translationRegistry.getNormalizedPrefixedTranslationBySingleton(DisjunctionKey.INSTANCE);

      if (orTranslation == null)
        throw new IllegalStateException("Could not locate translation for the OR operator in language " + targetLanguage);

      var predicateJoiner = new StringJoiner(" " + orTranslation + " ");

      for (var material : sortedMaterials) {
        var materialTranslation = translationRegistry.getNormalizedPrefixedTranslationBySingleton(material);

        if (materialTranslation == null)
          throw new IllegalStateException("Could not locate translation for " + material + " in language " + targetLanguage);

        predicateJoiner.add(materialTranslation);
      }

      var predicateString = predicateJoiner.toString();

      ItemPredicate predicate;

      try {
        var tokens = ippIntegration.predicateHelper.parseTokens(predicateString);
        predicate = ippIntegration.predicateHelper.parsePredicate(targetLanguage, tokens);
      } catch (ItemPredicateParseException e) {
        throw new IllegalStateException("Could not parse the predicate, despite it having been auto-generated");
      }

      pipePredicateEventHandler.setPredicate(pistonSign, new PredicateAndLanguage(predicate, targetLanguage));

      config.rootSection.pipes.predicates.command.generatePredicateSet.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("predicate", predicateString)
          .withVariable("set_command", "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET) + " " + predicateString)
          .withVariable("sign_x", pistonSign.getX())
          .withVariable("sign_y", pistonSign.getY())
          .withVariable("sign_z", pistonSign.getZ())
      );

      return true;
    }

    throw new IllegalStateException("Unaccounted-for command-action: " + normalizedAction.constant.name());
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(player) || args.length == 0)
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (normalizedAction.constant == CommandAction.SEARCH)
      return pipeSearchCommand.onTabComplete(sender, pipeSearchCommand.getCommand(), pipeSearchCommand.getCommand().getLabel(), Arrays.copyOfRange(args, 1, args.length));

    // Keep these aliases around, as they're already broadly known on our server
    switch (normalizedAction.constant) {
      case SET, SET_LANGUAGE, GET, REMOVE: {
        if (ippIntegration.mainCommand.getExecutor() instanceof TabCompleter tabCompleter)
          return tabCompleter.onTabComplete(sender, command, label, args);
      }
    }

    return List.of();
  }
}

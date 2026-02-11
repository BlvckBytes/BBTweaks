package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InvFilterCommand implements CommandExecutor, TabCompleter, Listener {

  private final Command command;
  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final PredicateHelper predicateHelper;

  private final NamespacedKey filterPredicateKey;
  private final NamespacedKey filterLanguageKey;
  private final NamespacedKey filterEnabledKey;

  private final Map<UUID, InventoryFilter> filterByPlayerId;

  public InvFilterCommand(
    Command command,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = command;
    this.plugin = plugin;
    this.config = config;

    ItemPredicateParserPlugin ipp;

    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
      throw new IllegalArgumentException("Expected plugin ItemPredicateParser to have been loaded at this point");

    this.predicateHelper = ipp.getPredicateHelper();

    this.filterPredicateKey = new NamespacedKey(plugin, "invfilter-predicate");
    this.filterLanguageKey = new NamespacedKey(plugin, "invfilter-language");
    this.filterEnabledKey = new NamespacedKey(plugin, "invfilter-enabled");

    this.filterByPlayerId = new HashMap<>();
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

    var pdc = player.getPersistentDataContainer();

    if (args.length == 0) {
      var currentFilter = filterByPlayerId.get(player.getUniqueId());
      String setFilterCommand = null;

      if (currentFilter != null && currentFilter.predicateAndLanguage != null) {
        var selectedLanguage = predicateHelper.getSelectedLanguage(player);
        var predicateLanguage = currentFilter.predicateAndLanguage.language();

        if (predicateLanguage == selectedLanguage)
          setFilterCommand = "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER) + " " + currentFilter.predicateString;
        else
          setFilterCommand = "/" + label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_FILTER_WITH_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(predicateLanguage) + " " + currentFilter.predicateString;
      }

      config.rootSection.invFilter.currentState.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("is_enabled", currentFilter != null && currentFilter.enabled)
          .withVariable("current_filter", currentFilter == null ? null : currentFilter.predicateString)
          .withVariable("set_filter_command", setFilterCommand)
      );

      return true;
    }

    var action = CommandAction.matcher.matchFirst(args[0]);

    if (action == null) {
      config.rootSection.invFilter.usageAction.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    if (action.constant == CommandAction.SET_FILTER || action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
      int argsOffset;
      TranslationLanguage language;

      if (action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
        me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant<TranslationLanguage> matchedLanguage;

        if (args.length == 1 || (matchedLanguage = TranslationLanguage.matcher.matchFirst(args[1])) == null) {
          config.rootSection.invFilter.usageLanguage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", action.getNormalizedName())
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
              .withVariable("action", action.getNormalizedName())
              .withVariable("language", TranslationLanguage.matcher.getNormalizedName(language))
          );

          return true;
        }
      }

      else {
        language = predicateHelper.getSelectedLanguage(player);
        argsOffset = 1;

        if (args.length == argsOffset) {
          config.rootSection.invFilter.usageFilterDefaultLanguage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", action.getNormalizedName())
          );

          return true;
        }
      }

      ItemPredicate predicate;

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        config.rootSection.invFilter.predicateError.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("error", predicateHelper.createExceptionMessage(e))
        );

        return true;
      }

      var newFilter = new InventoryFilter(new PredicateAndLanguage(predicate, language), true);

      filterByPlayerId.put(player.getUniqueId(), newFilter);

      pdc.set(filterPredicateKey, PersistentDataType.STRING, Objects.requireNonNull(newFilter.predicateString));
      pdc.set(filterLanguageKey, PersistentDataType.STRING, language.name());

      config.rootSection.invFilter.filterSetAndEnabled.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter", newFilter.predicateString)
      );

      return true;
    }

    if (action.constant == CommandAction.ENABLE) {
      var currentFilter = filterByPlayerId.get(player.getUniqueId());

      if (currentFilter == null || currentFilter.predicateString == null) {
        config.rootSection.invFilter.changeEnableNoFilterSet.sendMessage(player);
        return true;
      }

      if (currentFilter.enabled) {
        config.rootSection.invFilter.changeEnableEnabledAlready.sendMessage(player);
        return true;
      }

      var newFilter = new InventoryFilter(currentFilter.predicateAndLanguage, true);

      filterByPlayerId.put(player.getUniqueId(), newFilter);

      pdc.set(filterEnabledKey, PersistentDataType.BOOLEAN, true);

      config.rootSection.invFilter.filterEnabled.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter", newFilter.predicateString)
      );

      return true;
    }

    if (action.constant == CommandAction.DISABLE) {
      var currentFilter = filterByPlayerId.get(player.getUniqueId());

      if (currentFilter == null || currentFilter.predicateString == null) {
        config.rootSection.invFilter.changeEnableNoFilterSet.sendMessage(player);
        return true;
      }

      if (!currentFilter.enabled) {
        config.rootSection.invFilter.changeEnableDisabledAlready.sendMessage(player);
        return true;
      }

      var newFilter = new InventoryFilter(currentFilter.predicateAndLanguage, false);

      filterByPlayerId.put(player.getUniqueId(), newFilter);

      pdc.set(filterEnabledKey, PersistentDataType.BOOLEAN, false);

      config.rootSection.invFilter.filterDisabled.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter", newFilter.predicateString)
      );

      return true;
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

    if (action.constant == CommandAction.SET_FILTER || action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
      TranslationLanguage language;
      int argsOffset;

      if (action.constant == CommandAction.SET_FILTER_WITH_LANGUAGE) {
        if (args.length == 2)
          return TranslationLanguage.matcher.createCompletions(args[1]);

        var matchedLanguage = TranslationLanguage.matcher.matchFirst(args[1]);

        if (matchedLanguage == null)
          return List.of();

        language = matchedLanguage.constant;
        argsOffset = 2;
      }

      else {
        language = predicateHelper.getSelectedLanguage(player);
        argsOffset = 1;
      }

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        var completions = predicateHelper.createCompletion(language, tokens);

        if (completions.expandedPreviewOrError() != null)
          player.sendActionBar(completions.expandedPreviewOrError());

        return completions.suggestions();
      } catch (ItemPredicateParseException e) {
        player.sendActionBar(predicateHelper.createExceptionMessage(e));
        return List.of();
      }
    }

    return List.of();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPickup(PlayerAttemptPickupItemEvent event) {
    var filter = getOrLoadFilter(event.getPlayer());

    if (!filter.enabled || filter.predicateAndLanguage == null)
      return;

    if (!filter.predicateAndLanguage.predicate().test(event.getItem().getItemStack()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    var filter = getOrLoadFilter(player);

    if (!filter.enabled || filter.predicateAndLanguage == null)
      return;

    // Ensure that this important message is not spammed away by Essentials' MOTD and the like.
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var disableCommand = "/" + command.getLabel() + " " + CommandAction.matcher.getNormalizedName(CommandAction.DISABLE);

      config.rootSection.invFilter.activeFilterWarning.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("filter", filter.predicateString)
          .withVariable("disable_command", disableCommand)
      );
    }, 5L);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    filterByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private InventoryFilter getOrLoadFilter(Player player) {
    var playerId = player.getUniqueId();

    var filter = filterByPlayerId.get(playerId);

    if (filter != null)
      return filter;

    var pdc = player.getPersistentDataContainer();

    var predicateAndLanguage = loadPredicateFromPlayerPdc(player);

    var filterMode = pdc.get(filterEnabledKey, PersistentDataType.BOOLEAN);

    filter = new InventoryFilter(predicateAndLanguage, filterMode != null && filterMode);

    filterByPlayerId.put(playerId, filter);

    return filter;
  }

  private @Nullable PredicateAndLanguage loadPredicateFromPlayerPdc(Player player) {
    var pdc = player.getPersistentDataContainer();
    var filterLanguageString = pdc.get(filterLanguageKey, PersistentDataType.STRING);

    if (filterLanguageString == null)
      return null;

    TranslationLanguage language;

    try {
      language = TranslationLanguage.valueOf(filterLanguageString);
    } catch (Throwable e) {
      return null;
    }

    var filterPredicateString = pdc.get(filterPredicateKey, PersistentDataType.STRING);

    if (filterPredicateString == null || filterPredicateString.isBlank())
      return null;

    ItemPredicate predicate;

    try {
      var tokens = predicateHelper.parseTokens(filterPredicateString);
      predicate = predicateHelper.parsePredicate(language, tokens);
    } catch (ItemPredicateParseException e) {
      return null;
    }

    if (predicate == null)
      return null;

    return new PredicateAndLanguage(predicate, language);
  }
}

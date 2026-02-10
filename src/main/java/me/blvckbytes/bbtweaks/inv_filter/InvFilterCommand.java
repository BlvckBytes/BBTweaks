package me.blvckbytes.bbtweaks.inv_filter;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.item_predicate_parser.ItemPredicateParserPlugin;
import me.blvckbytes.item_predicate_parser.PredicateHelper;
import me.blvckbytes.item_predicate_parser.parse.ItemPredicateParseException;
import me.blvckbytes.item_predicate_parser.predicate.ItemPredicate;
import me.blvckbytes.item_predicate_parser.predicate.stringify.PlainStringifier;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InvFilterCommand implements CommandExecutor, TabCompleter, Listener {

  private final ConfigKeeper<MainSection> config;
  private final PredicateHelper predicateHelper;

  private final NamespacedKey filterPredicateKey;
  private final NamespacedKey filterLanguageKey;
  private final NamespacedKey filterModeKey;

  private final Map<UUID, InventoryFilter> filterByPlayerId;

  public InvFilterCommand(Plugin plugin, ConfigKeeper<MainSection> config) {
    this.config = config;

    ItemPredicateParserPlugin ipp;

    if (!Bukkit.getServer().getPluginManager().isPluginEnabled("ItemPredicateParser") || (ipp = ItemPredicateParserPlugin.getInstance()) == null)
      throw new IllegalArgumentException("Expected plugin ItemPredicateParser to have been loaded at this point");

    this.predicateHelper = ipp.getPredicateHelper();

    this.filterPredicateKey = new NamespacedKey(plugin, "invfilter-predicate");
    this.filterLanguageKey = new NamespacedKey(plugin, "invfilter-language");
    this.filterModeKey = new NamespacedKey(plugin, "invfilter-mode");

    this.filterByPlayerId = new HashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      // TODO: Config-message
      sender.sendMessage("§cPlayers only!");
      return true;
    }

    if (!player.hasPermission("bbtweaks.mechanic.invfilter")) {
      // TODO: Config-message
      player.sendMessage("§cNo permission!");
      return true;
    }

    var pdc = player.getPersistentDataContainer();

    if (args.length == 0) {
      var currentFilter = filterByPlayerId.get(player.getUniqueId());
      String filterString = null;
      String setFilterCommand = null;

      if (currentFilter != null && currentFilter.predicateAndLanguage() != null) {
        filterString = PlainStringifier.stringify(currentFilter.predicateAndLanguage().predicate(), true);

        var selectedLanguage = predicateHelper.getSelectedLanguage(player);
        var predicateLanguage = currentFilter.predicateAndLanguage().language();

        if (predicateLanguage == selectedLanguage)
          setFilterCommand = label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET) + " " + filterString;
        else
          setFilterCommand = label + " " + CommandAction.matcher.getNormalizedName(CommandAction.SET_LANGUAGE) + " " + TranslationLanguage.matcher.getNormalizedName(predicateLanguage) + " " + filterString;
      }

      var currentMode = currentFilter == null ? PredicateMode.OFF : currentFilter.mode();
      var modeName = PredicateMode.matcher.getNormalizedName(currentMode);

      // TODO: Config-message
      player.sendMessage("§aCurrent Filter: " + filterString);
      player.sendMessage("§aSet Filter Command: " + setFilterCommand);
      player.sendMessage("§aCurrent Mode: " + modeName);
      return true;
    }

    var action = CommandAction.matcher.matchFirst(args[0]);

    if (action == null) {
      // TODO: Config-message
      player.sendMessage("§cUsage: /" + label + " <" + CommandAction.matcher.createCompletions(null) + ">");
      return true;
    }

    if (action.constant == CommandAction.SET || action.constant == CommandAction.SET_LANGUAGE) {
      int argsOffset;
      TranslationLanguage language;

      if (action.constant == CommandAction.SET_LANGUAGE) {
        me.blvckbytes.item_predicate_parser.syllables_matcher.NormalizedConstant<TranslationLanguage> matchedLanguage;

        if (args.length == 1 || (matchedLanguage = TranslationLanguage.matcher.matchFirst(args[1])) == null) {
          // TODO: Config-message
          player.sendMessage("§cUsage: /" + label + " " + action.getNormalizedName() + " <" + TranslationLanguage.matcher.createCompletions(null) + "> <Filter>");
          return true;
        }

        language = matchedLanguage.constant;
        argsOffset = 2;

        if (args.length == argsOffset) {
          // TODO: Config-message
          player.sendMessage("§cUsage: /" + label + " " + action.getNormalizedName() + " " + TranslationLanguage.matcher.getNormalizedName(language) + " <Filter>");
          return true;
        }
      }

      else {
        language = predicateHelper.getSelectedLanguage(player);
        argsOffset = 1;

        if (args.length == argsOffset) {
          // TODO: Config-message
          player.sendMessage("§cUsage: /" + label + " " + action.getNormalizedName() + " <Filter>");
          return true;
        }
      }

      ItemPredicate predicate;

      try {
        var tokens = predicateHelper.parseTokens(args, argsOffset);
        predicate = predicateHelper.parsePredicate(language, tokens);
      } catch (ItemPredicateParseException e) {
        // TODO: Separate message specific to inv-filter
        config.rootSection.mechanic.magnet.filterCommandPredicateError.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("error", predicateHelper.createExceptionMessage(e))
        );

        return true;
      }

      var currentFilter = filterByPlayerId.get(player.getUniqueId());

      var newFilter = new InventoryFilter(
        new PredicateAndLanguage(predicate, language),
        currentFilter == null ? PredicateMode.OFF : currentFilter.mode()
      );

      filterByPlayerId.put(player.getUniqueId(), newFilter);

      var predicateString = PlainStringifier.stringify(predicate, true);

      pdc.set(filterPredicateKey, PersistentDataType.STRING, predicateString);
      pdc.set(filterLanguageKey, PersistentDataType.STRING, language.name());

      // TODO: Config-message
      player.sendMessage("§aFilter set to §2" + predicateString + "§a!");
      return true;
    }

    if (action.constant == CommandAction.MODE) {
      NormalizedConstant<PredicateMode> mode;

      if (args.length == 1 || (mode = PredicateMode.matcher.matchFirst(args[1])) == null) {
        player.sendMessage("§cUsage: /" + label + " " + action.getNormalizedName() + " <" + PredicateMode.matcher.createCompletions(null) + ">");
        return true;
      }

      var currentFilter = filterByPlayerId.get(player.getUniqueId());

      var newFilter = new InventoryFilter(
        currentFilter == null ? null : currentFilter.predicateAndLanguage(),
        mode.constant
      );

      filterByPlayerId.put(player.getUniqueId(), newFilter);

      pdc.set(filterModeKey, PersistentDataType.STRING, mode.constant.name());

      // TODO: Config-message
      player.sendMessage("§aMode set to §2" + mode.getNormalizedName() + "§a!");
      return true;
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return List.of();

    if (!player.hasPermission("bbtweaks.mechanic.invfilter"))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var action = CommandAction.matcher.matchFirst(args[0]);

    if (action == null)
      return List.of();

    if (action.constant == CommandAction.SET || action.constant == CommandAction.SET_LANGUAGE) {
      TranslationLanguage language;
      int argsOffset;

      if (action.constant == CommandAction.SET_LANGUAGE) {
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

    if (action.constant == CommandAction.MODE) {
      if (args.length == 2)
        return PredicateMode.matcher.createCompletions(args[1]);

      return List.of();
    }

    return List.of();
  }

  @EventHandler(ignoreCancelled = true)
  public void onPickup(PlayerAttemptPickupItemEvent event) {
    var filter = filterByPlayerId.get(event.getPlayer().getUniqueId());

    if (filter == null || filter.predicateAndLanguage() == null || filter.mode() == PredicateMode.OFF)
      return;

    var testResult = filter.predicateAndLanguage().predicate().test(event.getItem().getItemStack());
    var doAllow = filter.mode() == PredicateMode.ALLOW;

    if (doAllow && !testResult || !doAllow && testResult)
      event.setCancelled(true);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    filterByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();
    var pdc = player.getPersistentDataContainer();

    var predicateAndLanguage = loadPredicateFromPlayerPdc(player);

    var filterModeString = pdc.get(filterModeKey, PersistentDataType.STRING);
    PredicateMode filterMode = PredicateMode.OFF;

    if (filterModeString != null) {
      try {
        filterMode = PredicateMode.valueOf(filterModeString);
      } catch (Throwable ignored) {}
    }

    if (predicateAndLanguage != null || filterMode != PredicateMode.OFF)
      filterByPlayerId.put(player.getUniqueId(), new InventoryFilter(predicateAndLanguage, filterMode));
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

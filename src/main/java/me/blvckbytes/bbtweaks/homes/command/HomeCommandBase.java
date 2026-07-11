package me.blvckbytes.bbtweaks.homes.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.homes.HomesSection;
import me.blvckbytes.bbtweaks.homes.storage.HomesStorage;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.item_predicate_parser.syllables_matcher.Syllables;
import me.blvckbytes.item_predicate_parser.syllables_matcher.SyllablesMatcher;
import me.blvckbytes.item_predicate_parser.translation.TranslatedLangKeyed;
import me.blvckbytes.item_predicate_parser.translation.TranslationRegistry;
import me.blvckbytes.item_predicate_parser.translation.keyed.LangKeyedItemMaterial;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class HomeCommandBase implements CommandHandler {

  protected final PluginCommand command;
  private final Function<HomesSection, CommandSection> sectionSelector;

  protected final JavaPlugin plugin;
  protected final ConfigKeeper<MainSection> config;
  protected final HomesStorage homesStorage;

  protected HomeCommandBase(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    HomesStorage homesStorage,
    String initialName,
    Function<HomesSection, CommandSection> sectionSelector
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(initialName));
    this.sectionSelector = sectionSelector;

    this.plugin = plugin;
    this.config = config;
    this.homesStorage = homesStorage;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return sectionSelector.apply(config.rootSection.homes);
  }

  protected abstract void onHomeNameParsedCommand(@NotNull Player sender, @NotNull Command command, @NotNull String label, @Nullable HomeParameter homeParameter, @NotNull String @NotNull [] args);

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.homes.playersOnly.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      onHomeNameParsedCommand(player, command, label, null, args);
      return true;
    }

    var homeName = args[0];
    var separatorIndex = homeName.indexOf(':');

    if (separatorIndex < 0) {
      onHomeNameParsedCommand(player, command, label, new HomeParameter(null, homeName), withoutFirstArgument(args));
      return true;
    }

    if (!(player.hasPermission("bbtweaks.homes.others"))) {
      config.rootSection.homes.cannotUseColonInName.sendMessage(sender);
      return true;
    }

    var targetPlayerName = homeName.substring(0, separatorIndex);

    if (targetPlayerName.isEmpty()) {
      config.rootSection.homes.targetPlayerNameCannotBeEmpty.sendMessage(sender);
      return true;
    }

    var targetPlayer = homesStorage.getKnownPlayerByName(targetPlayerName);

    if (targetPlayer == null) {
      config.rootSection.homes.targetPlayerNameIsUnknown.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", targetPlayerName)
      );

      return true;
    }

    if (separatorIndex == homeName.length() - 1) {
      config.rootSection.homes.targetHomeNameCannotBeEmpty.sendMessage(sender);
      return true;
    }

    homeName = homeName.substring(separatorIndex + 1);

    if (reportInvalidHomeName(player, homeName))
      return true;

    onHomeNameParsedCommand(player, command, label, new HomeParameter(targetPlayer, homeName), withoutFirstArgument(args));
    return true;
  }

  protected boolean reportInvalidHomeName(Player recipient, String homeName) {
    if (!homeName.contains(":"))
      return false;

    config.rootSection.homes.cannotUseColonInName.sendMessage(recipient);
    return true;
  }

  public abstract @Nullable List<String> onHomeNameParsedTabComplete(@NotNull Player sender, @NotNull Command command, @NotNull String label, @NotNull HomeParameter homeParameter, @NotNull String @NotNull [] args);

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || args.length == 0)
      return List.of();

    var homeName = args[0];
    var separatorIndex = homeName.indexOf(':');

    if (separatorIndex < 0) {
      if (args.length > 1)
        return onHomeNameParsedTabComplete(player, command, label, new HomeParameter(null, homeName), withoutFirstArgument(args));

      return homesStorage.accessHomes(player).getHomeNames().stream()
        .filter(name -> StringUtils.startsWithIgnoreCase(name, homeName))
        .limit(15)
        .toList();
    }

    var targetPlayerName = homeName.substring(0, separatorIndex);
    var targetPlayer = homesStorage.getKnownPlayerByName(targetPlayerName);

    if (targetPlayer == null)
      return List.of();

    var targetHomeName = separatorIndex == homeName.length() - 1 ? "" : homeName.substring(separatorIndex + 1);

    if (args.length > 1)
      return onHomeNameParsedTabComplete(player, command, label, new HomeParameter(targetPlayer, targetHomeName), withoutFirstArgument(args));

    return homesStorage.accessHomes(targetPlayer).getHomeNames().stream()
      .filter(name -> StringUtils.startsWithIgnoreCase(name, targetHomeName))
      .limit(15)
      .map(name -> targetPlayerName + ":" + name)
      .toList();
  }

  private static String[] withoutFirstArgument(String[] args) {
    return Arrays.copyOfRange(args, 1, args.length);
  }

  protected @Nullable TranslatedLangKeyed<LangKeyedItemMaterial> shortestOrNull(List<TranslatedLangKeyed<LangKeyedItemMaterial>> items) {
    if (items.isEmpty())
      return null;

    items.sort((a, b) -> {
      var aLength = a.normalizedPrefixedTranslation.length();
      var bLength = b.normalizedPrefixedTranslation.length();

      if (aLength != bLength)
        return aLength - bLength;

      return a.alphabeticalIndex - b.alphabeticalIndex;
    });

    return items.getFirst();
  }

  protected List<TranslatedLangKeyed<LangKeyedItemMaterial>> matchMaterialsBySyllables(IPPIntegration ippIntegration, Player player, String input) {
    var result = new ArrayList<TranslatedLangKeyed<LangKeyedItemMaterial>>();

    var language = ippIntegration.predicateHelper.getSelectedLanguage(player);
    var registry = ippIntegration.languageRegistry.getTranslationRegistry(language);
    var translatedMaterials = ((TranslationRegistry) registry).lookup(LangKeyedItemMaterial.class);

    var inputSyllables = Syllables.forString(input, Syllables.DELIMITER_SEARCH_PATTERN);
    var matcher = new SyllablesMatcher();

    matcher.setQuery(inputSyllables);

    for (var translatedMaterial : translatedMaterials) {
      matcher.resetTargetMatches();
      matcher.setTarget(translatedMaterial.syllables);

      matcher.match();

      if (matcher.hasUnmatchedQuerySyllables())
        continue;

      result.add(translatedMaterial);
    }

    return result;
  }

  protected List<String> buildMaterialsSuggestions(IPPIntegration ippIntegration, Player sender, String input) {
    return matchMaterialsBySyllables(ippIntegration, sender, input).stream()
      .limit(15)
      .map(it -> it.normalizedUnPrefixedTranslation)
      .toList();
  }
}

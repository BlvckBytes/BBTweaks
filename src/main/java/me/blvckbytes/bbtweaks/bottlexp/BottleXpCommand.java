package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.Material;
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
import java.util.stream.Stream;

public class BottleXpCommand implements CommandHandler {

  private static final int[] SUGGESTION_PERCENTAGES = { 100, 75, 50, 25 };

  private final PluginCommand command;
  private final AutoPickupContainerListener autoPickupContainerListener;
  private final ConfigKeeper<MainSection> config;

  public BottleXpCommand(
    JavaPlugin plugin,
    AutoPickupContainerListener autoPickupContainerListener,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(BottleXpCommandSection.INITIAL_NAME));
    this.autoPickupContainerListener = autoPickupContainerListener;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.bottleXp.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.bottleXp.noPermission.sendMessage(sender);
      return true;
    }

    var experiencePerBottle = config.rootSection.bottleXp.experiencePerBottle;
    final var availableExperience = calculateAvailableExperience(player);

    if (availableExperience < experiencePerBottle) {
      config.rootSection.bottleXp.hasNoExperienceToBottle.sendMessage(player);
      return true;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("available_experience", availableExperience)
      .withVariable("experience_per_bottle", experiencePerBottle)
      .withVariable("stack_size", Material.EXPERIENCE_BOTTLE.getMaxStackSize());

    if (args.length == 0) {
      var suggestions = makeOverviewSuggestions(availableExperience, experiencePerBottle);

      config.rootSection.bottleXp.experienceOverview.sendMessage(
        player,
        environment
          .withVariable("label", label)
          .withVariable("suggestions", suggestions)
          .withVariable("available_level", player.getLevel())
          .withVariable("stack_size", Material.EXPERIENCE_BOTTLE.getMaxStackSize())
          .withVariable("storages", BottleStorage.matcher.createCompletions(null))
      );

      return true;
    }

    var normalizedStorage = BottleStorage.matcher.getNormalizedConstant(BottleStorage.DEFAULT_VALUE);

    if (args.length > 2 || (args.length > 1 && (normalizedStorage = BottleStorage.matcher.matchFirst(args[1])) == null)) {
      config.rootSection.bottleXp.commandUsage.sendMessage(
        player,
        environment
          .withVariable("label", label)
          .withVariable("storages", BottleStorage.matcher.createCompletions(null))
      );

      return true;
    }

    environment
      .withVariable("use_inventory", normalizedStorage.constant.intoInventory)
      .withVariable("use_shulkers", normalizedStorage.constant.intoShulkers);

    var limitString = args[0];
    var isLimitPercentage = false;

    if (limitString.endsWith("%")) {
      limitString = limitString.substring(0, limitString.length() - 1);
      isLimitPercentage = true;
    }

    int numericLimit;

    try {
      numericLimit = Integer.parseInt(limitString);

      if (numericLimit <= 0)
        throw new IllegalArgumentException();
    } catch (Throwable e) {
      config.rootSection.bottleXp.invalidMaximumValue.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", limitString)
      );

      return true;
    }

    var maximumExperience = numericLimit;

    if (isLimitPercentage) {
      if (numericLimit > 100) {
        config.rootSection.bottleXp.maximumPercentageTooHigh.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("percentage", numericLimit)
        );

        return true;
      }

      maximumExperience = (int) (availableExperience * (numericLimit / 100D));
    }

    environment
      .withVariable("maximum_experience", maximumExperience)
      .withVariable("maximum_percentage", isLimitPercentage ? numericLimit : null);

    if (maximumExperience > availableExperience) {
      config.rootSection.bottleXp.maximumValueExceedsAvailable.sendMessage(player, environment);
      return true;
    }

    if (maximumExperience < experiencePerBottle) {
      config.rootSection.bottleXp.maximumValueBelowExpPerBottle.sendMessage(player, environment);
      return true;
    }

    var handoutSession = new BottleHandoutSession(player, normalizedStorage.constant, autoPickupContainerListener);

    if (normalizedStorage.constant.intoShulkers && !handoutSession.encounteredShulkerBoxes()) {
      config.rootSection.bottleXp.carriesNoShulkerBoxes.sendMessage(player, environment);
      return true;
    }

    var bottledExperience = 0;
    var bottleCount = 0;
    var wasSpaceExhausted = false;

    while (maximumExperience - bottledExperience >= experiencePerBottle) {
      if (!handoutSession.tryAddABottle()) {
        wasSpaceExhausted = true;
        break;
      }

      ++bottleCount;
      bottledExperience += experiencePerBottle;
    }

    if (bottledExperience == 0) {
      config.rootSection.bottleXp.cannotHoldAnyBottles.sendMessage(player, environment);
      return true;
    }

    var levelBefore = player.getLevel();
    player.setLevel(0);
    player.setExp(0);
    player.giveExp(availableExperience - bottledExperience);
    var levelAfter = player.getLevel();

    handoutSession.onCompletion();

    config.rootSection.bottleXp.afterBottling.sendMessage(
      player,
      environment
        .withVariable("bottle_count", bottleCount)
        .withVariable("bottled_experience", bottledExperience)
        .withVariable("level_before", levelBefore)
        .withVariable("level_after", levelAfter)
        .withVariable("exhausted_space", wasSpaceExhausted)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(player))
      return List.of();

    if (args.length == 1) {
      return Stream.concat(
        Stream.of(String.valueOf(calculateAvailableExperience(player))),
        Arrays.stream(SUGGESTION_PERCENTAGES).mapToObj(String::valueOf).map(it -> it + "%")
      )
        .filter(it -> it.startsWith(args[0]))
        .toList();
    }

    if (args.length == 2)
      return BottleStorage.matcher.createCompletions(args[1]);

    return List.of();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.bottleXp.command;
  }

  private @NotNull List<OverviewSuggestion> makeOverviewSuggestions(int availableExperience, int experiencePerBottle) {
    var suggestions = new ArrayList<OverviewSuggestion>();
    var suggestedBottleCounts = new IntArraySet();

    for (var suggestionPercentage : SUGGESTION_PERCENTAGES) {
      var experience = (int) (availableExperience * (suggestionPercentage / 100D));

      if (experience < experiencePerBottle)
        continue;

      var bottleCount = experience / experiencePerBottle;

      if (!suggestedBottleCounts.add(bottleCount))
        continue;

      var bottledExperience = bottleCount * experiencePerBottle;

      suggestions.add(new OverviewSuggestion(
        suggestionPercentage,
        bottledExperience,
        experience / experiencePerBottle,
        getLevelFromExperiencePoints(availableExperience - bottledExperience)
      ));
    }
    return suggestions;
  }

  private int getLevelFromExperiencePoints(int experience) {
    var remainingExperience = experience;
    var level = 0;

    while (true) {
      var requiredExperience = getExperiencePointsNeededForLevel(level);

      if (requiredExperience > remainingExperience)
        break;

      remainingExperience -= requiredExperience;
      ++level;
    }

    return level;
  }

  // See: https://minecraft.wiki/w/Experience#Leveling_up
  private int getExperiencePointsNeededForLevel(int lvl) {
    if (lvl <= 15)
      return 2 * lvl + 7;

    if (lvl <= 30)
      return 5 * lvl - 38;

    return 9 * lvl - 158;
  }

  private int calculateAvailableExperience(Player player) {
    var currentLevelPercentage = player.getExp();
    var currentLevelExperience = player.getExpToLevel();

    var availableExperience = (int) (currentLevelPercentage * currentLevelExperience);
    var currentLevel = player.getLevel();

    // We've already accounted for the initial level by the getExp percentage.
    while (--currentLevel >= 0)
      availableExperience += getExperiencePointsNeededForLevel(currentLevel);

    return availableExperience;
  }
}

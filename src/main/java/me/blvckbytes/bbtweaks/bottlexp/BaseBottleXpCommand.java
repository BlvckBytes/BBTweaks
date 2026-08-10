package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class BaseBottleXpCommand implements CommandHandler {

  private static final int[] SUGGESTION_PERCENTAGES = { 100, 75, 50, 25 };

  protected final ConfigKeeper<MainSection> config;

  protected final Supplier<ComponentMarkup> overviewMessageSupplier;

  protected BaseBottleXpCommand(
    ConfigKeeper<MainSection> config,
    Supplier<ComponentMarkup> overviewMessageSupplier
  ) {
    this.config = config;
    this.overviewMessageSupplier = overviewMessageSupplier;
  }

  protected abstract void handleBottling(
    Player player,
    String label,
    String[] args,
    int maximumExperience,
    int availableExperience,
    InterpretationEnvironment environment
  );

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

      overviewMessageSupplier.get().sendMessage(
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

    handleBottling(player, label, args, maximumExperience, availableExperience, environment);
    return true;
  }

  protected abstract List<String> handleRemainingTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args);

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

    return handleRemainingTabComplete(sender, command, label, args);
  }

  protected void setExperience(Player player, int experience) {
    player.setLevel(0);
    player.setExp(0);
    player.giveExp(experience);
  }

  // See: https://minecraft.wiki/w/Experience#Leveling_up
  protected int getExperiencePointsNeededForLevel(int lvl) {
    if (lvl <= 15)
      return 2 * lvl + 7;

    if (lvl <= 30)
      return 5 * lvl - 38;

    return 9 * lvl - 158;
  }

  protected int calculateAvailableExperience(Player player) {
    var currentLevelPercentage = player.getExp();
    var currentLevelExperience = player.getExpToLevel();

    var availableExperience = (int) (currentLevelPercentage * currentLevelExperience);
    var currentLevel = player.getLevel();

    // We've already accounted for the initial level by the getExp percentage.
    while (--currentLevel >= 0)
      availableExperience += getExperiencePointsNeededForLevel(currentLevel);

    return availableExperience;
  }

  protected int getLevelFromExperiencePoints(int experience) {
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
}

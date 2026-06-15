package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.un_craft.SpaceSimulator;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
  private final ConfigKeeper<MainSection> config;

  public BottleXpCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(BottleXpCommandSection.INITIAL_NAME));
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

    if (args.length == 0) {
      var suggestions = makeOverviewSuggestions(availableExperience, experiencePerBottle);

      config.rootSection.bottleXp.experienceOverview.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("suggestions", suggestions)
          .withVariable("experience_per_bottle", experiencePerBottle)
          .withVariable("available_experience", availableExperience)
          .withVariable("available_level", player.getLevel())
          .withVariable("stack_size", Material.EXPERIENCE_BOTTLE.getMaxStackSize())
      );

      return true;
    }

    // Let's allow for player's specifying "50 %" instead of "50%".
    var parameter = String.join("", args);
    var isPercentage = false;

    // Let's be lenient here as well, allowing for multiple mistyped percent-signs.
    while (parameter.endsWith("%")) {
      parameter = parameter.substring(0, parameter.length() - 1);
      isPercentage = true;
    }

    int numericParameter;

    try {
      numericParameter = Integer.parseInt(parameter);

      if (numericParameter <= 0)
        throw new IllegalArgumentException();
    } catch (Throwable e) {
      config.rootSection.bottleXp.invalidMaximumValue.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("input", parameter)
      );

      return true;
    }

    var maximumExperience = numericParameter;

    if (isPercentage) {
      if (numericParameter > 100) {
        config.rootSection.bottleXp.maximumPercentageTooHigh.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("percentage", numericParameter)
        );

        return true;
      }

      maximumExperience = (int) (availableExperience * (numericParameter / 100D));
    }

    if (maximumExperience > availableExperience) {
      config.rootSection.bottleXp.maximumValueExceedsAvailable.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("maximum_experience", maximumExperience)
          .withVariable("available_experience", availableExperience)
      );

      return true;
    }

    if (maximumExperience < experiencePerBottle) {
      config.rootSection.bottleXp.maximumValueBelowExpPerBottle.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("maximum_experience", maximumExperience)
          .withVariable("experience_per_bottle", experiencePerBottle)
      );

      return true;
    }

    var spaceSimulator = new SpaceSimulator(player.getInventory(), ItemStack::getType);

    var bottledExperience = 0;
    var bottleCount = 0;

    while (maximumExperience - bottledExperience >= experiencePerBottle) {
      spaceSimulator.addItem(Material.EXPERIENCE_BOTTLE, 1);

      if (spaceSimulator.didDropItems())
        break;

      ++bottleCount;
      bottledExperience += experiencePerBottle;
    }

    if (bottledExperience == 0) {
      config.rootSection.bottleXp.cannotHoldAnyBottles.sendMessage(player);
      return true;
    }

    var levelBefore = player.getLevel();
    player.setLevel(0);
    player.setExp(0);
    player.giveExp(availableExperience - bottledExperience);
    var levelAfter = player.getLevel();

    var remainingBottleCount = bottleCount;

    while (remainingBottleCount > 0) {
      var dropCount = Math.min(remainingBottleCount, Material.EXPERIENCE_BOTTLE.getMaxStackSize());

      player.getInventory()
        .addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, dropCount))
        .values()
        .forEach(player::dropItem);

      remainingBottleCount -= dropCount;
    }

    config.rootSection.bottleXp.afterBottling.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("bottle_count", bottleCount)
        .withVariable("bottled_experience", bottledExperience)
        .withVariable("available_experience", availableExperience)
        .withVariable("maximum_experience", maximumExperience)
        .withVariable("maximum_percentage", isPercentage ? numericParameter : null)
        .withVariable("level_before", levelBefore)
        .withVariable("level_after", levelAfter)
        .withVariable("inventory_full", spaceSimulator.didDropItems())
        .withVariable("stack_size", Material.EXPERIENCE_BOTTLE.getMaxStackSize())
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

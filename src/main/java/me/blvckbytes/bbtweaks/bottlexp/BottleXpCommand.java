package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class BottleXpCommand extends BaseBottleXpCommand {

  private final PluginCommand command;
  private final AutoPickupContainerListener autoPickupContainerListener;

  public BottleXpCommand(
    JavaPlugin plugin,
    AutoPickupContainerListener autoPickupContainerListener,
    ConfigKeeper<MainSection> config
  ) {
    super(config, () -> config.rootSection.bottleXp.experienceOverview);

    this.command = Objects.requireNonNull(plugin.getCommand(BottleXpCommandSection.INITIAL_NAME));
    this.autoPickupContainerListener = autoPickupContainerListener;
  }

  @Override
  protected void handleBottling(
    Player player,
    String label,
    String[] args,
    int maximumExperience,
    int availableExperience,
    InterpretationEnvironment environment
  ) {
    var normalizedStorage = BottleStorage.matcher.getNormalizedConstant(BottleStorage.DEFAULT_VALUE);

    if (args.length > 2 || (args.length > 1 && (normalizedStorage = BottleStorage.matcher.matchFirst(args[1])) == null)) {
      config.rootSection.bottleXp.commandUsage.sendMessage(
        player,
        environment
          .withVariable("label", label)
          .withVariable("storages", BottleStorage.matcher.createCompletions(null))
      );

      return;
    }

    environment
      .withVariable("use_inventory", normalizedStorage.constant.intoInventory)
      .withVariable("use_shulkers", normalizedStorage.constant.intoShulkers);

    var handoutSession = new BottleHandoutSession(player, normalizedStorage.constant, autoPickupContainerListener);

    if (normalizedStorage.constant.intoShulkers && !handoutSession.encounteredShulkerBoxes()) {
      config.rootSection.bottleXp.carriesNoShulkerBoxes.sendMessage(player, environment);
      return;
    }

    var experiencePerBottle = config.rootSection.bottleXp.experiencePerBottle;

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
      return;
    }

    var levelBefore = player.getLevel();
    setExperience(player, availableExperience - bottledExperience);
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
  }

  @Override
  protected List<String> handleRemainingTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
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
}

package me.blvckbytes.bbtweaks.auto_pickup_container.command;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_pickup_container.LazyContainer;
import me.blvckbytes.bbtweaks.auto_pickup_container.MaterialCounts;
import me.blvckbytes.bbtweaks.auto_pickup_container.settings.AutoPickupContainerSettingsStore;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.integration.ipp.IPPIntegration;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Tag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AutoPickupContainerCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final IPPIntegration ippIntegration;
  private final AutoPickupContainerSettingsStore settingsStore;
  private final AutoPickupContainerListener containerListener;

  public AutoPickupContainerCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    IPPIntegration ippIntegration,
    AutoPickupContainerSettingsStore settingsStore,
    AutoPickupContainerListener containerListener
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(AutoPickupContainerCommandSection.INITIAL_NAME));

    this.config = config;
    this.ippIntegration = ippIntegration;
    this.settingsStore = settingsStore;
    this.containerListener = containerListener;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.autoPickupContainer.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.autoPickupContainer.command.playersOnly.sendMessage(sender);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      sendActionUsage(player, label);
      return true;
    }

    var settings = settingsStore.accessSettings(player);

    switch (normalizedAction.constant) {
      case ON, OFF, TOGGLE -> {
        if (args.length != 1) {
          sendActionUsage(player, label);
          return true;
        }

        settings.setEnabled(switch (normalizedAction.constant) {
          case ON -> true;
          case OFF -> false;
          default -> null;
        });
      }

      case OVERVIEW -> {
        if (args.length != 1) {
          sendActionUsage(player, label);
          return true;
        }

        // TODO: Would be really nice to use stackability as a count-key instead and then display in a readonly pageable UI

        var totalCounts = new MaterialCounts(new HashMap<>());
        var containerCount = 0;

        for (var item : player.getInventory().getContents()) {
          if (item == null)
            continue;

          if (!Tag.SHULKER_BOXES.isTagged(item.getType()))
            continue;

          if (!containerListener.doesContainMarker(item.getPersistentDataContainer()))
            continue;

          var shulkerInventory = LazyContainer.tryAccessInventory(item);

          if (shulkerInventory == null)
            continue;

          totalCounts.addCountsFrom(MaterialCounts.fromInventory(shulkerInventory));
          ++containerCount;
        }

        if (containerCount == 0) {
          config.rootSection.autoPickupContainer.overviewNoContainers.sendMessage(player);
          return true;
        }

        var environment = new InterpretationEnvironment()
          .withVariable("container_count", containerCount)
          .withVariable("item_counts", totalCounts.asSortedTranslatedCountList(ippIntegration));

        if (totalCounts.counts().isEmpty()) {
          config.rootSection.autoPickupContainer.overviewAllContainersEmpty.sendMessage(player, environment);
          return true;
        }

        config.rootSection.autoPickupContainer.overviewScreen.sendMessage(player, environment);
      }

      case CAPACITY_WARNING -> {
        NormalizedConstant<CapacityWarningMode> normalizedMode;

        if (args.length != 2 || (normalizedMode = CapacityWarningMode.matcher.matchFirst(args[1])) == null) {
          config.rootSection.autoPickupContainer.command.capacityWarningUsage.sendMessage(
            player,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
              .withVariable("warning_modes", CapacityWarningMode.matcher.createCompletions(null))
          );

          return true;
        }

        settings.selectCapacityWarningMode(normalizedMode.constant);
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player) || args.length == 0)
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (args.length == 2 && normalizedAction.constant == CommandAction.CAPACITY_WARNING)
      return CapacityWarningMode.matcher.createCompletions(args[1]);

    return List.of();
  }

  private void sendActionUsage(Player player, String label) {
    config.rootSection.autoPickupContainer.command.commandUsage.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("label", label)
        .withVariable("actions", CommandAction.matcher.createCompletions(null))
    );
  }
}

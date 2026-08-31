package me.blvckbytes.bbtweaks.no_ai;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.AutoWirer;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.no_ai.simulator.BehaviorSimulator;
import me.blvckbytes.bbtweaks.no_ai.simulator.VillagerBehaviorSimulator;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

public class NoAiCommand implements CommandHandler, Tickable {

  private static final long BEHAVIOR_PROCESS_PERIOD_T = 35;

  private final PluginCommand command;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final List<BehaviorSimulator<?>> behaviorSimulators;

  public NoAiCommand(
    AutoWirer wirer,
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) throws Throwable {
    this.command = Objects.requireNonNull(plugin.getCommand(NoAiCommandSection.INITIAL_NAME));

    this.plugin = plugin;
    this.config = config;

    this.behaviorSimulators = new ArrayList<>();
    this.behaviorSimulators.add(wirer.withSingletonAndGet(VillagerBehaviorSimulator.class));
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % BEHAVIOR_PROCESS_PERIOD_T != 0)
      return;

    var timeCache = TimeCache.captureCurrentTimes();

    for (var behaviorSimulator : behaviorSimulators)
      behaviorSimulator.tick(timeCache);
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.noAi.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.noAi.playersOnly.sendMessage(sender);
      return true;
    }

    var rayTraceResult = player.rayTraceEntities(5);

    if (rayTraceResult == null || rayTraceResult.getHitEntity() == null) {
      config.rootSection.noAi.notLookingAtAnEntity.sendMessage(player);
      return true;
    }

    var rayTracedEntity = rayTraceResult.getHitEntity();

    if (!canBuildAt(player, rayTracedEntity.getLocation().getBlock())) {
      config.rootSection.noAi.cannotBuildHere.sendMessage(player);
      return true;
    }

    BehaviorSimulator<?> targetBehaviorSimulator = null;

    for (var behaviorSimulator : behaviorSimulators) {
      if (!behaviorSimulator.isHandledInstance(rayTracedEntity))
        continue;

      targetBehaviorSimulator = behaviorSimulator;
      break;
    }

    if (targetBehaviorSimulator == null) {
      config.rootSection.noAi.notLookingAtSupportedEntity.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("unsupported_entity_key", rayTracedEntity.getType().translationKey())
          .withVariable(
            "supported_entity_keys",
            behaviorSimulators.stream()
              .map(behaviorSimulator -> behaviorSimulator.entityType.translationKey())
              .toList()
          )
      );

      return true;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", (int) rayTracedEntity.getX())
      .withVariable("y", (int) rayTracedEntity.getY())
      .withVariable("z", (int) rayTracedEntity.getZ())
      .withVariable("entity_key", rayTracedEntity.getType().translationKey());

    if (args.length == 0) {
      var newState = targetBehaviorSimulator.toggleAndGetIfNowPresent(rayTracedEntity);

      if (newState) {
        config.rootSection.noAi.aiNowDisabled.sendMessage(player, environment);
        return true;
      }

      config.rootSection.noAi.aiNowEnabled.sendMessage(player, environment);
      return true;
    }

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length != 1 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.noAi.commandActionUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    if (!targetBehaviorSimulator.isEntityIdInSimulatedList(rayTracedEntity)) {
      config.rootSection.noAi.entityIsNotSimulated.sendMessage(player, environment);
      return true;
    }

    switch (normalizedAction.constant) {
      case STATUS -> {
        targetBehaviorSimulator.sendStatusMessage(player, rayTracedEntity);
      }

      case MAKE_LOOK -> {
        // Unreachable, as a matching simulator was found.
        if (!(rayTracedEntity instanceof Mob mob))
          return true;

        var entityLocation = mob.getEyeLocation();
        entityLocation.setDirection(player.getEyeLocation().toVector().subtract(entityLocation.toVector()));
        rayTracedEntity.setRotation(entityLocation.getYaw(), entityLocation.getPitch());

        config.rootSection.noAi.entityIsNowLookingAtExecutor.sendMessage(player, environment);
      }
    }

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }

  private void callFakeEvent(Event event) {
    for (var listener : event.getHandlers().getRegisteredListeners()) {
      if (listener.getPlugin().equals(plugin))
        continue;

      try {
        listener.callEvent(event);
      } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + listener.getPlugin().getName(), e);
      }
    }
  }

  private boolean canBuildAt(Player player, Block block) {
    //noinspection UnstableApiUsage
    var fakePlaceEvent = new BlockPlaceEvent(block, block.getState(), block, new ItemStack(Material.DIRT), player, false, EquipmentSlot.HAND);
    callFakeEvent(fakePlaceEvent);
    return !fakePlaceEvent.isCancelled();
  }
}

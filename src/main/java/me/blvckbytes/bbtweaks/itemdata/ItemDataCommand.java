package me.blvckbytes.bbtweaks.itemdata;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.itemdata.display.ItemDataDisplayHandler;
import me.blvckbytes.bbtweaks.util.EmptyObject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ItemDataCommand implements CommandHandler {

  private final PluginCommand command;
  private final ConfigKeeper<MainSection> config;
  private final ItemDataDisplayHandler itemDataDisplayHandler;

  public ItemDataCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    ItemDataDisplayHandler itemDataDisplayHandler
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(ItemDataCommandSection.INITIAL_NAME));
    this.config = config;
    this.itemDataDisplayHandler = itemDataDisplayHandler;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.itemData.command;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.itemData.playersOnly.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      var targetItem = player.getInventory().getItemInMainHand();

      if (targetItem.getType().isAir()) {
        config.rootSection.itemData.noItemInMainHand.sendMessage(sender);
        return true;
      }

      var environment = ItemDataAccessor.makeEnvironmentIfHasData(targetItem);

      if (environment == null) {
        config.rootSection.itemData.heldItemHasNoData.sendMessage(sender);
        return true;
      }

      config.rootSection.itemData.heldItemScreen.sendMessage(player, environment);
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null || args.length > 1) {
      config.rootSection.itemData.commandUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    if (normalizedAction.constant == CommandAction.INV) {
      if (Arrays.stream(player.getInventory().getStorageContents()).allMatch(it -> it == null || it.getType().isAir())) {
        config.rootSection.itemData.noItemInInventory.sendMessage(sender);
        return true;
      }

      itemDataDisplayHandler.show(player, EmptyObject.INSTANCE);
      return true;
    }

    throw new IllegalStateException("Unaccounted-for command-action: " + normalizedAction.constant);
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }
}

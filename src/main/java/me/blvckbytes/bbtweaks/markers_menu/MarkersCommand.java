package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayData;
import me.blvckbytes.bbtweaks.markers_menu.display.MarkerDisplayHandler;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MarkersCommand implements CommandExecutor, TabCompleter {

  private final MarkerDisplayHandler markerDisplayHandler;
  private final ConfigKeeper<MainSection> config;

  public MarkersCommand(
    MarkerDisplayHandler markerDisplayHandler,
    ConfigKeeper<MainSection> config
  ) {
    this.markerDisplayHandler = markerDisplayHandler;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.markersMenu.messages.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.markers")) {
      config.rootSection.markersMenu.messages.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length == 0) {
      config.rootSection.markersMenu.messages.showingCategories.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("category_count", config.rootSection.markersMenu.categories.size())
      );

      markerDisplayHandler.show(player, new MarkerDisplayData(null, null));
      return true;
    }

    var category = config.rootSection.markersMenu.getCategoryByNameIgnoreCase(args[0]);

    if (category == null) {
      config.rootSection.markersMenu.messages.unknownCategory.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("name", args[0])
          .withVariable("available_categories", config.rootSection.markersMenu.categories.keySet())
      );

      return true;
    }

    config.rootSection.markersMenu.messages.showingMarkers.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("name", category.getDisplayNameOrName())
        .withVariable("marker_count", category._members.size())
    );

    markerDisplayHandler.show(player, new MarkerDisplayData(category, null));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length != 1)
      return List.of();

    return config.rootSection.markersMenu.categories.keySet().stream()
      .filter(category -> StringUtils.startsWithIgnoreCase(category, args[0]))
      .limit(10)
      .toList();
  }
}

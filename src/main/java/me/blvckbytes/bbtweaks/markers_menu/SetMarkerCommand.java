package me.blvckbytes.bbtweaks.markers_menu;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetMarkerCommand implements CommandExecutor, TabCompleter {

  private final ConfigKeeper<MainSection> config;
  private final Logger logger;

  public SetMarkerCommand(ConfigKeeper<MainSection> config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.markersMenu.messages.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("bbtweaks.setmarker")) {
      config.rootSection.markersMenu.messages.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length != 1) {
      config.rootSection.markersMenu.messages.setMarkerUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

    var marker = config.rootSection.markersMenu.getMarkerByNameIgnoreCase(args[0]);

    if (marker == null) {
      config.rootSection.markersMenu.messages.unknownMarker.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("name", args[0])
          .withVariable("available_markers", config.rootSection.markersMenu.markers.keySet())
      );

      return true;
    }

    var location = player.getLocation();

    marker.setLocation(location);

    try {
      writeLocationToConfig(marker);
    } catch (Throwable e) {
      config.rootSection.markersMenu.messages.markerSetError.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("name", marker.getDisplayNameOrName())
      );

      logger.log(Level.SEVERE, "An error occurred while trying to write a marker-location to the config", e);
      return true;
    }

    config.rootSection.markersMenu.messages.markerSetSuccessfully.sendMessage(
      player,
      new InterpretationEnvironment()
        .withVariable("name", marker.getDisplayNameOrName())
    );

    return true;
  }

  private void writeLocationToConfig(MarkerSection markerSection) throws Exception {
    var location = markerSection.location;

    if (location == null)
      throw new IllegalStateException("There was no previously set location on the marker-POJO");

    var configMapper = config.getConfigMapper();
    var yamlConfig = configMapper.getConfig();
    var markerName = markerSection._name;

    var commonPath = "markersMenu.markers." + markerName + ".location";

    yamlConfig.set(commonPath + ".x", location.x);
    yamlConfig.set(commonPath + ".y", location.y);
    yamlConfig.set(commonPath + ".z", location.z);
    yamlConfig.set(commonPath + ".yaw", location.yaw);
    yamlConfig.set(commonPath + ".pitch", location.pitch);
    yamlConfig.set(commonPath + ".world", location.world);

    configMapper.saveConfig();
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length != 1 || !sender.hasPermission("bbtweaks.setmarker"))
      return List.of();

    return config.rootSection.markersMenu.markers.keySet().stream()
      .filter(category -> StringUtils.startsWithIgnoreCase(category, args[0]))
      .limit(10)
      .toList();
  }
}

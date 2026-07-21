package me.blvckbytes.bbtweaks.back;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BackOverrideCommand implements CommandHandler, Listener, Tickable {

  private final PluginCommand command;
  private final LocationHistoryStore locationHistoryStore;
  private final ConfigKeeper<MainSection> config;

  private long time;

  private record IgnoredId(UUID playerId, long addStamp) {}

  private final List<IgnoredId> ignoredIds;

  public BackOverrideCommand(
    JavaPlugin plugin,
    LocationHistoryStore locationHistoryStore,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand("back"));
    this.locationHistoryStore = locationHistoryStore;
    this.config = config;

    this.ignoredIds = new ArrayList<>();
  }

  public void temporarilyIgnore(Player player) {
    ignoredIds.add(new IgnoredId(player.getUniqueId(), time));
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.backOverride.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission("essentials.back")) {
      config.rootSection.backOverride.noPermission.sendMessage(player);
      return true;
    }

    var lastLocation = locationHistoryStore.accessHistory(player).getNthLastLocation(0);

    if (lastLocation == null) {
      config.rootSection.backOverride.noLastLocation.sendMessage(player);
      return true;
    }

    player.teleport(lastLocation);
    config.rootSection.backOverride.teleportedBack.sendMessage(player);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreProcess(PlayerCommandPreprocessEvent event) {
    // Do not override if we have no last location yet (initial case).
    if (locationHistoryStore.accessHistory(event.getPlayer()).getNthLastLocation(0) == null)
      return;

    var message = event.getMessage();
    var firstSpaceIndex = message.indexOf(' ');

    if (firstSpaceIndex < 0) {
      if (message.equals("/back"))
        event.setMessage("/bbtweaks:back");

      return;
    }

    var commandToken = message.substring(0, firstSpaceIndex);

    if (commandToken.equals("/back"))
      event.setMessage("/bbtweaks:back" + message.substring(firstSpaceIndex));
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    var player = event.getPlayer();

    if (!player.hasPermission("essentials.back.onteleport"))
      return;

    if (player.hasMetadata("NPC") || !(event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND))
      return;

    //noinspection deprecation - f you, Paper!
    if (player.getMetadata("essentials:ignore-teleport").stream().anyMatch(MetadataValue::asBoolean))
      return;

    if (isIgnored(player))
      return;

    var history = locationHistoryStore.accessHistory(player);
    var addEvent = new LocationHistoryAddEvent(player, history, player.getLocation());

    Bukkit.getPluginManager().callEvent(addEvent);

    if (!addEvent.isCancelled())
      history.add(addEvent.getLocation());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getPlayer();

    if (!player.hasPermission("essentials.back.ondeath"))
      return;

    locationHistoryStore.accessHistory(player).add(player.getLocation());
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return null;
  }

  @Override
  public void tick(long relativeTime) {
    time = relativeTime;

    for (var index = ignoredIds.size() - 1; index >= 0; --index) {
      var ignoredId = ignoredIds.get(index);

      if (relativeTime - ignoredId.addStamp > 0)
        ignoredIds.remove(index);
    }
  }

  private boolean isIgnored(Player player) {
    var playerId = player.getUniqueId();

    for (var ignoredId : ignoredIds) {
      if (playerId.equals(ignoredId.playerId))
        return true;
    }

    return false;
  }
}

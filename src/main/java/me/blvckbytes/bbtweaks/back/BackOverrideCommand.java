package me.blvckbytes.bbtweaks.back;

import me.blvckbytes.bbtweaks.BBTweaksPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BackOverrideCommand implements CommandExecutor, TabCompleter, Listener {

  private final BBTweaksPlugin bbTweaksPlugin;
  private final LastLocationStore lastLocationStore;

  public BackOverrideCommand(BBTweaksPlugin bbTweaksPlugin, LastLocationStore lastLocationStore) {
    this.bbTweaksPlugin = bbTweaksPlugin;
    this.lastLocationStore = lastLocationStore;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (!player.hasPermission("essentials.back"))
      return true;

    var lastLocation = lastLocationStore.getLastLocation(player);

    if (lastLocation == null)
      return true;

    player.teleport(lastLocation);
    player.sendMessage(bbTweaksPlugin.accessConfigValue("chat.backCommandMessage"));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPreProcess(PlayerCommandPreprocessEvent event) {
    // Do not override if we have no last location yet (initial case).
    if (lastLocationStore.getLastLocation(event.getPlayer()) == null)
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

    lastLocationStore.setLastLocation(player, player.getLocation());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    var player = event.getPlayer();

    if (!player.hasPermission("essentials.back.ondeath"))
      return;

    lastLocationStore.setLastLocation(player, player.getLocation());
  }
}

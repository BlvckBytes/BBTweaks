package me.blvckbytes.bbtweaks;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BackOverrideCommand implements CommandExecutor, TabCompleter, Listener {

  private final BBTweaksPlugin bbTweaksPlugin;
  private final LastLocationStore lastLocationStore;

  private List<List<String>> ignoreList;

  public BackOverrideCommand(BBTweaksPlugin bbTweaksPlugin, LastLocationStore lastLocationStore) {
    this.bbTweaksPlugin = bbTweaksPlugin;
    this.lastLocationStore = lastLocationStore;

    bbTweaksPlugin.registerConfigReloadListener(this::loadBackInListenerIgnoreList);
    this.loadBackInListenerIgnoreList();
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

    if (isTeleportListenerIgnored())
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

  private boolean isTeleportListenerIgnored() {
    if (ignoreList.isEmpty())
      return false;

    for (StackTraceElement stackElement : Thread.currentThread().getStackTrace()) {
      String representation = (stackElement.getClassName() + "#" + stackElement.getMethodName()).toLowerCase();

      if (ignoreList.stream().anyMatch(keywords -> keywords.stream().allMatch(representation::contains)))
        return true;
    }

    return false;
  }

  private void loadBackInListenerIgnoreList() {
    this.ignoreList = new ArrayList<>();

    for (String entry : bbTweaksPlugin.getConfiguration().getStringList("back-in-listener-ignore-list")) {
      List<String> ignoreEntry = new ArrayList<>();

      for (String keyword : entry.split(",")) {
        keyword = keyword.trim();

        if (!keyword.isEmpty())
          ignoreEntry.add(keyword.toLowerCase());
      }

      if (!ignoreEntry.isEmpty())
        ignoreList.add(ignoreEntry);
    }
  }
}

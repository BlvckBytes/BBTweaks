package me.blvckbytes.bbtweaks.back;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public class BacktrackCommand implements CommandHandler, Tickable, Listener {

  private static final int TITLE_UPDATE_PERIOD_T = 5;

  private final PluginCommand command;

  private final LocationHistoryStore locationHistoryStore;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final Map<UUID, BacktrackSession> sessionByPlayerId;

  public BacktrackCommand(JavaPlugin plugin, LocationHistoryStore locationHistoryStore, ConfigKeeper<MainSection> config) {
    this.command = Objects.requireNonNull(plugin.getCommand(BacktrackCommandSection.INITIAL_NAME));

    this.locationHistoryStore = locationHistoryStore;
    this.config = config;
    this.plugin = plugin;
    this.sessionByPlayerId = new HashMap<>();
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

    var playerId = player.getUniqueId();

    if (sessionByPlayerId.containsKey(playerId)) {
      config.rootSection.backOverride.backtrackAlreadyInASession.sendMessage(player);
      return true;
    }

    var history = locationHistoryStore.accessHistory(player);
    var lastLocation = history.getNthLastLocation(0);

    if (lastLocation == null) {
      config.rootSection.backOverride.noLastLocation.sendMessage(player);
      return true;
    }

    sessionByPlayerId.put(playerId, BacktrackSession.captureHistoryAndStartAtOrigin(plugin, player, label, history));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    return List.of();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var session = sessionByPlayerId.remove(event.getPlayer().getUniqueId());

    if (session != null)
      session.onEnd(false, false);
  }

  @EventHandler
  public void onFlyToggle(PlayerToggleFlightEvent event) {
    // Prevent accidental toggles while quickly navigating through the history
    if (sessionByPlayerId.containsKey(event.getPlayer().getUniqueId()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onInput(PlayerInputEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    var input = event.getInput();

    if (input.isForward() || input.isBackward() || input.isLeft() || input.isRight()) {
      var session = sessionByPlayerId.remove(playerId);

      if (session == null)
        return;

      session.onEnd(true, true);
      return;
    }

    if (input.isJump() || input.isSneak()) {
      var session = sessionByPlayerId.get(playerId);

      if (session == null)
        return;

      if (input.isJump()) {
        session.next();
        return;
      }

      session.previous();
    }
  }

  @EventHandler
  public void onHistoryAdd(LocationHistoryAddEvent event) {
    var player = event.getPlayer();
    var session = sessionByPlayerId.remove(player.getUniqueId());

    if (session != null) {
      session.onEnd(true, false);
      event.setLocation(session.initialLocation);
      config.rootSection.backOverride.backtrackCancelledDueToExternalTeleport.sendMessage(player);
    }
  }

  private void tickSessions(long relativeTime) {
    ComponentMarkup markup;

    for (var session : sessionByPlayerId.values()) {
      var environment = session.makeEnvironment();

      if (relativeTime % TITLE_UPDATE_PERIOD_T == 0) {
        var hasTitleOrSubtitle = false;

        if ((markup = config.rootSection.backOverride.backtrackTitle) != null) {
          hasTitleOrSubtitle = true;
          session.player.sendTitlePart(
            TitlePart.TITLE,
            markup.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst()
          );
        }

        if ((markup = config.rootSection.backOverride.backtrackSubtitle) != null) {
          hasTitleOrSubtitle = true;
          session.player.sendTitlePart(
            TitlePart.SUBTITLE,
            markup.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst()
          );
        }

        if (hasTitleOrSubtitle) {
          session.player.sendTitlePart(
            TitlePart.TIMES,
            Title.Times.times(Duration.ZERO, Duration.ofMillis(2 * TITLE_UPDATE_PERIOD_T * 50), Duration.ZERO)
          );
        }
      }

      // Always update the action-bar, seeing how it could otherwise be shadowed by temp-fly, jobs, etc.
      if ((markup = config.rootSection.backOverride.backtrackActionBar) != null)
        markup.sendActionBar(session.player);
    }
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.backOverride.backtrackCommand;
  }

  @Override
  public void tick(long relativeTime) {
    tickSessions(relativeTime);
  }
}

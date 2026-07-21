package me.blvckbytes.bbtweaks.auto_fly;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AutoFlyCommand implements CommandHandler, Listener {

  private static final String ESSENTIALS_FLY_PERMISSION = "essentials.fly";

  private final PluginCommand command;
  private final NamespacedKey keyMode;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  public AutoFlyCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(AutoFlyCommandSection.INITIAL_NAME));
    this.keyMode = new NamespacedKey(plugin, "auto-fly-mode");

    this.plugin = plugin;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.autoFly.playersOnly.sendMessage(sender);
      return true;
    }

    if (!player.hasPermission(ESSENTIALS_FLY_PERMISSION)) {
      config.rootSection.autoFly.noFlyPermission.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
      );

      return true;
    }

    if (args.length == 0) {
      config.rootSection.autoFly.currentMode.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("mode", AutoMode.matcher.getNormalizedName(readModeFor(player)))
      );

      return true;
    }

    NormalizedConstant<AutoMode> normalizedMode;

    if ((normalizedMode = AutoMode.matcher.matchFirst(args[0])) == null) {
      config.rootSection.autoFly.commandUsage.sendMessage(
        player,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("modes", AutoMode.matcher.createCompletions(null))
      );

      return true;
    }

    player.getPersistentDataContainer().set(keyMode, PersistentDataType.STRING, normalizedMode.constant.name());

    handleAutoFly(player);

    var message = switch (normalizedMode.constant) {
      case DISABLED -> config.rootSection.autoFly.newModeOff;
      case ENABLED -> config.rootSection.autoFly.newModeEnabled;
      case ENABLED_SET_FLYING -> config.rootSection.autoFly.newModeEnabledSetFlying;
    };

    message.sendMessage(player);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !player.hasPermission(ESSENTIALS_FLY_PERMISSION))
      return List.of();

    if (args.length == 1)
      return AutoMode.matcher.createCompletions(args[0]);

    return List.of();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> handleAutoFly(event.getPlayer()), 5);
  }

  @EventHandler
  public void onTeleport(PlayerTeleportEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> handleAutoFly(event.getPlayer()), 1);
  }

  @EventHandler
  public void onGameModeChange(PlayerGameModeChangeEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> handleAutoFly(event.getPlayer()), 1);
  }

  private void handleAutoFly(Player player) {
    if (!player.hasPermission(ESSENTIALS_FLY_PERMISSION))
      return;

    var mode = readModeFor(player);

    if (mode == AutoMode.DISABLED)
      return;

    applyAutoFlyMode(player, mode, false);
  }

  private void applyAutoFlyMode(Player player, AutoMode mode, boolean isRetry) {
    if (isRetry) {
      // It seems like we're fighting against some external sort of plugin-initiated reset here, so let's
      // retry to once again apply the mode if it has been cleared despite the player having permission.
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (player.getAllowFlight() || !player.hasPermission(ESSENTIALS_FLY_PERMISSION))
          return;

        player.setAllowFlight(true);

        if (mode == AutoMode.ENABLED_SET_FLYING)
          player.setFlying(true);
      }, 1);

      return;
    }

    if (mode == AutoMode.ENABLED_SET_FLYING) {
      player.setVelocity(new Vector(0, .1, 0));

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        player.setAllowFlight(true);
        player.setFlying(true);

        applyAutoFlyMode(player, mode, true);
      }, 1);

      return;
    }

    if (mode == AutoMode.ENABLED) {
      player.setAllowFlight(true);

      applyAutoFlyMode(player, mode, true);
    }
  }

  private AutoMode readModeFor(Player player) {
    var modeValue = player.getPersistentDataContainer().get(keyMode, PersistentDataType.STRING);
    var result = AutoMode.DISABLED;

    if (modeValue != null) {
      try {
        result = AutoMode.valueOf(modeValue);
      } catch (Throwable ignored) {}
    }

    return result;
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.autoFly.command;
  }
}

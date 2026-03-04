package me.blvckbytes.bbtweaks.auto_fly;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.util.NameScopedKeyValueStore;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AutoFlyCommand implements CommandExecutor, TabCompleter, Listener {

  private static final String ESSENTIALS_FLY_PERMISSION = "essentials.fly";
  private static final String KEY_AUTO_FLY = "auto-fly";

  private final Plugin plugin;
  private final NameScopedKeyValueStore preferencesStore;
  private final ConfigKeeper<MainSection> config;

  public AutoFlyCommand(
    Plugin plugin,
    NameScopedKeyValueStore preferencesStore,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.preferencesStore = preferencesStore;
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

    preferencesStore.write(player.getUniqueId().toString(), KEY_AUTO_FLY, normalizedMode.constant.name());

    handleAutoFly(player);

    var message = switch (normalizedMode.constant) {
      case OFF -> config.rootSection.autoFly.newModeOff;
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

  private void handleAutoFly(Player player) {
    if (!player.hasPermission(ESSENTIALS_FLY_PERMISSION))
      return;

    var mode = readModeFor(player);

    if (mode == AutoMode.OFF)
      return;

    if (mode == AutoMode.ENABLED_SET_FLYING) {
      player.setVelocity(new Vector(0, .1, 0));

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        player.setAllowFlight(true);
        player.setFlying(true);
      }, 1);

      return;
    }

    if (mode == AutoMode.ENABLED)
      player.setAllowFlight(true);
  }

  private AutoMode readModeFor(Player player) {
    var modeValue = preferencesStore.read(player.getUniqueId().toString(), KEY_AUTO_FLY);
    var result = AutoMode.OFF;

    if (modeValue != null) {
      try {
        result = AutoMode.valueOf(modeValue);
      } catch (Throwable ignored) {}
    }

    return result;
  }
}

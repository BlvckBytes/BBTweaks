package me.blvckbytes.bbtweaks.pipes.command.wireless_pipe;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.CommandHandler;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.pipes.PipeBlockCacheRegistry;
import me.blvckbytes.bbtweaks.pipes.TubeColor;
import me.blvckbytes.bbtweaks.pipes.WirelessPipeSign;
import me.blvckbytes.bbtweaks.pipes.predicates.PipePredicateEventHandler;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class WirelessPipeCommand implements CommandHandler, Listener, Tickable {

  private static final String EXIT_SENTINEL = "exit";

  private enum InvalidSignReason {
    MARKER_LINE_NOT_BLANK_OR_MARKER,
    NOT_MOUNTED_ON_GLASS,
    IS_ALREADY_CONNECTED,
  }

  private final PluginCommand command;

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;
  private final PipeBlockCacheRegistry cacheRegistry;
  private final PipePredicateEventHandler predicateEventHandler;

  private final Map<UUID, InteractionSession> interactionSessionByPlayerId;

  private long relativeTime;

  public WirelessPipeCommand(
    JavaPlugin plugin,
    ConfigKeeper<MainSection> config,
    PipeBlockCacheRegistry cacheRegistry,
    PipePredicateEventHandler predicateEventHandler
  ) {
    this.command = Objects.requireNonNull(plugin.getCommand(WirelessPipeCommandSection.INITIAL_NAME));

    this.plugin = plugin;
    this.config = config;
    this.cacheRegistry = cacheRegistry;
    this.predicateEventHandler = predicateEventHandler;

    this.interactionSessionByPlayerId = new HashMap<>();
  }

  @Override
  public PluginCommand getCommand() {
    return command;
  }

  @Override
  public @Nullable CommandSection getCommandSection() {
    return config.rootSection.pipes.wirelessPipeCommand;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.pipes.wirelessPipeCommand.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.pipes.wirelessPipeCommand.noPermission.sendMessage(sender);
      return true;
    }

    if (args.length != 0) {
      if (args.length != 1 || !args[0].equalsIgnoreCase(EXIT_SENTINEL)) {
        config.rootSection.pipes.wirelessPipeCommand.exitUsage.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("label", label)
            .withVariable("exit_sentinel", EXIT_SENTINEL)
        );

        return true;
      }

      var cancelledSession = interactionSessionByPlayerId.remove(player.getUniqueId());

      if (cancelledSession == null) {
        config.rootSection.pipes.wirelessPipeCommand.notCurrentlyInASession.sendMessage(
          player,
          new InterpretationEnvironment()
            .withVariable("label", label)
        );

        return true;
      }

      clearActionBarNextTick(player);

      config.rootSection.pipes.wirelessPipeCommand.currentSessionExited.sendMessage(player);
      return true;
    }

    var hitResult = player.getWorld().rayTraceBlocks(
      player.getEyeLocation(),
      player.getEyeLocation().getDirection(),
      5, FluidCollisionMode.NEVER, false
    );

    Block currentBlock;

    if (hitResult == null || (currentBlock = hitResult.getHitBlock()) == null || !(currentBlock.getState(false) instanceof Sign currentSign)) {
      config.rootSection.pipes.wirelessPipeCommand.notLookingAtASign.sendMessage(sender);
      return true;
    }

    var environment = new InterpretationEnvironment()
      .withVariable("x", currentBlock.getX())
      .withVariable("y", currentBlock.getY())
      .withVariable("z", currentBlock.getZ());

    var currentSignInvalidReason = getInvalidSignReason(currentSign);

    if (currentSignInvalidReason != null) {
      switch (currentSignInvalidReason) {
        case IS_ALREADY_CONNECTED -> config.rootSection.pipes.wirelessPipeCommand.lookedAtSignAlreadyConnected.sendMessage(player, environment);
        case NOT_MOUNTED_ON_GLASS -> config.rootSection.pipes.wirelessPipeCommand.lookedAtSignNotMountedOnGlass.sendMessage(player, environment);
        case MARKER_LINE_NOT_BLANK_OR_MARKER -> config.rootSection.pipes.wirelessPipeCommand.lookedAtSignIncompatibleMarker.sendMessage(player, environment);
      }

      return true;
    }

    if (!predicateEventHandler.canEditSign(player, currentSign)) {
      config.rootSection.pipes.wirelessPipeCommand.cannotEditSign.sendMessage(sender, environment);
      return true;
    }

    var session = interactionSessionByPlayerId.get(player.getUniqueId());

    if (session == null) {
      session = new InteractionSession(player, relativeTime, currentBlock, label);
      interactionSessionByPlayerId.put(player.getUniqueId(), session);

      config.rootSection.pipes.wirelessPipeCommand.selectedFirstSign.sendMessage(
        sender,
        environment
          .withVariable("label", label)
          .withVariable("exit_sentinel", EXIT_SENTINEL)
      );

      return true;
    }

    if (session.firstSignBlock().equals(currentBlock)) {
      config.rootSection.pipes.wirelessPipeCommand.cannotSelectFirstSignTwice.sendMessage(sender);
      return true;
    }

    interactionSessionByPlayerId.remove(player.getUniqueId());

    clearActionBarNextTick(session.player());

    if (!(session.firstSignBlock().getState(false) instanceof Sign firstSign)) {
      config.rootSection.pipes.wirelessPipeCommand.firstSignIsGone.sendMessage(
        sender,
        environment
          .withVariable("x", session.firstSignBlock().getX())
          .withVariable("y", session.firstSignBlock().getY())
          .withVariable("z", session.firstSignBlock().getZ())
      );

      return true;
    }

    var firstSignInvalidReason = getInvalidSignReason(firstSign);

    if (firstSignInvalidReason != null) {
      var firstSignEnvironment = new InterpretationEnvironment()
        .withVariable("x", session.firstSignBlock().getX())
        .withVariable("y", session.firstSignBlock().getY())
        .withVariable("z", session.firstSignBlock().getZ());

      switch (firstSignInvalidReason) {
        case IS_ALREADY_CONNECTED -> config.rootSection.pipes.wirelessPipeCommand.firstSignAlreadyConnected.sendMessage(player, firstSignEnvironment);
        case NOT_MOUNTED_ON_GLASS -> config.rootSection.pipes.wirelessPipeCommand.firstSignNotMountedOnGlass.sendMessage(player, firstSignEnvironment);
        case MARKER_LINE_NOT_BLANK_OR_MARKER -> config.rootSection.pipes.wirelessPipeCommand.firstSignIncompatibleMarker.sendMessage(player, firstSignEnvironment);
      }

      return true;
    }

    if (firstSign.getWorld() != currentSign.getWorld()) {
      config.rootSection.pipes.wirelessPipeCommand.secondSignDifferentWorld.sendMessage(player, environment);
      return true;
    }

    var blockCache = cacheRegistry.getBlockCache(player.getWorld());

    var firstSide = firstSign.getSide(Side.FRONT);

    firstSide.line(1, Component.text(WirelessPipeSign.MARKER));
    firstSide.line(2, Component.text(currentSign.getX() + " " + currentSign.getY() + " " + currentSign.getZ()));

    firstSign.update(true, false);
    blockCache.invalidateCache(firstSign.getBlock());

    var currentSide = currentSign.getSide(Side.FRONT);

    currentSide.line(1, Component.text(WirelessPipeSign.MARKER));
    currentSide.line(2, Component.text(firstSign.getX() + " " + firstSign.getY() + " " + firstSign.getZ()));

    currentSign.update(true, false);
    blockCache.invalidateCache(currentSign.getBlock());

    config.rootSection.pipes.wirelessPipeCommand.wirelessConnectionEstablished.sendMessage(
      sender,
      environment
        .withVariable("first_x", session.firstSignBlock().getX())
        .withVariable("first_y", session.firstSignBlock().getY())
        .withVariable("first_z", session.firstSignBlock().getZ())
        .withVariable("second_x", currentBlock.getX())
        .withVariable("second_y", currentBlock.getY())
        .withVariable("second_z", currentBlock.getZ())
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player) || !command.testPermission(sender))
      return List.of();

    if (args.length == 1 && interactionSessionByPlayerId.containsKey(player.getUniqueId()))
      return List.of(EXIT_SENTINEL);

    return List.of();
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    interactionSessionByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    if (relativeTime % 5 != 0)
      return;

    for (var iterator = interactionSessionByPlayerId.values().iterator(); iterator.hasNext();) {
      var session = iterator.next();

      if (relativeTime - session.createdAt() > config.rootSection.pipes.wirelessPipeCommand.interactionSessionTimeoutTicks) {
        config.rootSection.pipes.wirelessPipeCommand.interactionSessionTimeout.sendMessage(session.player());
        iterator.remove();
        clearActionBarNextTick(session.player());
        continue;
      }

      config.rootSection.pipes.wirelessPipeCommand.secondBlockActionBarPrompt.sendActionBar(
        session.player(),
        new InterpretationEnvironment()
          .withVariable("selected_x", session.firstSignBlock().getX())
          .withVariable("selected_y", session.firstSignBlock().getY())
          .withVariable("selected_z", session.firstSignBlock().getZ())
          .withVariable("label", session.commandLabel())
      );
    }
  }

  private void clearActionBarNextTick(Player player) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> player.sendActionBar(Component.text(" ")), 1);
  }

  private @Nullable InvalidSignReason getInvalidSignReason(Sign sign) {
    var targetSide = sign.getSide(Side.FRONT);
    var markerContents = ComponentUtil.asTrimmedText(targetSide.line(1));

    if (!markerContents.isBlank() && !markerContents.equalsIgnoreCase(WirelessPipeSign.MARKER))
      return InvalidSignReason.MARKER_LINE_NOT_BLANK_OR_MARKER;

    if (!(sign.getBlockData() instanceof Directional directional))
      return InvalidSignReason.NOT_MOUNTED_ON_GLASS;

    var mountBlock = sign.getBlock().getRelative(directional.getFacing().getOppositeFace());
    var tubeColor = TubeColor.fromMaterial(mountBlock.getType());

    if (tubeColor.color() == TubeColor.NONE)
      return InvalidSignReason.NOT_MOUNTED_ON_GLASS;

    if (isSignConnected(sign))
      return InvalidSignReason.IS_ALREADY_CONNECTED;

    return null;
  }

  private boolean isSignConnected(Sign sign) {
    var blockCache = cacheRegistry.getBlockCache(sign.getWorld());
    var thisSignBlock = sign.getBlock();

    try {
      // Ensure that the sign is loaded.
      thisSignBlock.getState(false);

      var thisWirelessSign = blockCache.getWirelessPipeSign(thisSignBlock, blockCache.getCachedBlock(thisSignBlock));

      if (thisWirelessSign != null) {
        // Ensure that the sign is loaded.
        thisWirelessSign.referencedBlock.getState(false);

        var otherWirelessSign = blockCache.getWirelessPipeSign(thisWirelessSign.referencedBlock, blockCache.getCachedBlock(thisWirelessSign.referencedBlock));

        if (otherWirelessSign != null)
          return true;
      }
    } catch (Throwable _) {}

    return false;
  }
}

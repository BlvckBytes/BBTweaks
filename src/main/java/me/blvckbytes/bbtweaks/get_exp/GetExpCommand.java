package me.blvckbytes.bbtweaks.get_exp;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.furnace_level_display.FurnaceLevelDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

public class GetExpCommand implements CommandExecutor, TabCompleter, Listener {

  // The interaction session-logic below is pretty much a fork of that within ItemPredicateParser.
  // We could extract that into a utility one day, but so far, that'd be overkill.

  private final FurnaceLevelDisplay furnaceLevelDisplay;
  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  private final Map<UUID, GetExpInteractionSession> interactionSessionByPlayerId;
  private final Object2LongMap<UUID> lastEventCancelTimeByPlayerId;

  private long relativeTime;

  public GetExpCommand(
    FurnaceLevelDisplay furnaceLevelDisplay,
    Plugin plugin,
    ConfigKeeper<MainSection> config
  ) {
    this.furnaceLevelDisplay = furnaceLevelDisplay;
    this.plugin = plugin;
    this.config = config;

    Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);

    this.interactionSessionByPlayerId = new HashMap<>();
    this.lastEventCancelTimeByPlayerId = new Object2LongOpenHashMap<>();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.getExp.playersOnly.sendMessage(sender);
      return true;
    }

    if (!command.testPermission(player)) {
      config.rootSection.getExp.noPermission.sendMessage(sender);
      return true;
    }

    var playerId = player.getUniqueId();

    if (interactionSessionByPlayerId.containsKey(playerId)) {
      config.rootSection.getExp.alreadyInASession.sendMessage(player);
      return true;
    }

    var dropOrb = args.length > 0 && args[0].equalsIgnoreCase("orb");

    if (dropOrb && !player.hasPermission("bbtweaks.getexp.orb")) {
      config.rootSection.getExp.noPermissionOrb.sendMessage(player);
      return true;
    }

    interactionSessionByPlayerId.put(playerId, new GetExpInteractionSession(player, block -> {
      var environment = new InterpretationEnvironment()
        .withVariable("x", block.getX())
        .withVariable("y", block.getY())
        .withVariable("z", block.getZ());

      if (!(block.getState() instanceof Furnace furnace)) {
        config.rootSection.getExp.notAFurnace.sendMessage(player, environment);
        return;
      }

      var totalExperience = furnaceLevelDisplay.calculateExperience(player, furnace.getRecipesUsed());

      var addedExperience = (int) Math.round(totalExperience);

      if (addedExperience <= 0) {
        config.rootSection.getExp.noExperienceStored.sendMessage(player, environment);
        return;
      }

      furnace.setRecipesUsed(new HashMap<>());
      furnace.update(true, false);

      var levelBefore = player.getLevel();

      if (!dropOrb) {
        player.giveExp(addedExperience);
        sendInfoTitle(player, environment, levelBefore, addedExperience);
        return;
      }

      var orb = player.getWorld().spawn(player.getLocation().add(0, .5, 0), ExperienceOrb.class);
      orb.setExperience(addedExperience);

      Bukkit.getScheduler().runTaskLater(plugin, () -> sendInfoTitle(player, environment, levelBefore, addedExperience), 5L);
    }));

    config.rootSection.getExp.sessionInitialized.sendMessage(player);
    return true;
  }

  private void sendInfoTitle(Player player, InterpretationEnvironment environment, int levelBefore, int addedExperience) {
    var levelAfter = player.getLevel();

    player.sendTitlePart(TitlePart.TITLE, Component.empty());

    player.sendTitlePart(
      TitlePart.SUBTITLE,
      config.rootSection.getExp.retrievedFromFurnaceSubtitle.interpret(
        SlotType.SINGLE_LINE_CHAT,
        environment
          .withVariable("added_experience", addedExperience)
          .withVariable("level_before", levelBefore)
          .withVariable("level_after", levelAfter)
      ).getFirst()
    );

    player.sendTitlePart(TitlePart.TIMES, Title.Times.times(
      Duration.ofMillis(120),
      Duration.ofMillis(1000),
      Duration.ofMillis(120)
    ));
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (sender.hasPermission("bbtweaks.getexp.orb") && args.length == 1)
      return List.of("orb");

    return List.of();
  }

  public void tick() {
    ++relativeTime;

    if (relativeTime % 10 != 0)
      return;

    var actionBarSignal = config.rootSection.getExp.interactionMultiModeActionBarSignal;

    for (var iterator = interactionSessionByPlayerId.values().iterator(); iterator.hasNext();) {
      var session = iterator.next();
      var expirySeconds = config.rootSection.getExp.interactionExpirySeconds;

      if (session.isExpired(expirySeconds)) {
        iterator.remove();

        config.rootSection.getExp.interactionExpired.sendMessage(
          session.player,
          new InterpretationEnvironment()
            .withVariable("expiry_seconds", expirySeconds)
        );

        if (session.allowMultiUse && actionBarSignal != null)
          session.player.sendActionBar(Component.empty()); // Immediately clear action-bar signal

        continue;
      }

      if (session.allowMultiUse && actionBarSignal != null)
        actionBarSignal.sendActionBar(session.player);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var playerId = event.getPlayer().getUniqueId();
    interactionSessionByPlayerId.remove(playerId);
    lastEventCancelTimeByPlayerId.removeLong(playerId);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent event) {
    var player = event.getPlayer();
    var action = event.getAction();

    if (player.isSneaking() && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
      var interactionSession = interactionSessionByPlayerId.get(player.getUniqueId());

      if (interactionSession != null) {
        event.setCancelled(true);

        if (!interactionSession.allowMultiUse) {
          interactionSession.allowMultiUse = true;
          interactionSession.touchExpiry();
          config.rootSection.getExp.interactionMultiModeEntered.sendMessage(player);
          return;
        }

        interactionSessionByPlayerId.remove(player.getUniqueId());
        player.sendActionBar(Component.empty()); // Immediately clear action-bar signal
        config.rootSection.getExp.interactionMultiModeExited.sendMessage(player);
        return;
      }
    }

    if (!(action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK))
      return;

    if(event.getHand() == EquipmentSlot.OFF_HAND)
      return;

    var clickedBlock = event.getClickedBlock();

    if (clickedBlock == null)
      return;

    if (handleInteractionSessionAndGetIfCancel(player, clickedBlock))
      event.setCancelled(true);
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    var player = event.getPlayer();

    if (handleInteractionSessionAndGetIfCancel(player, event.getBlockAgainst()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    var player = event.getPlayer();

    if (handleInteractionSessionAndGetIfCancel(player, event.getBlock()))
      event.setCancelled(true);
  }

  @EventHandler
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    var player = event.getPlayer();

    if (handleInteractionSessionAndGetIfCancel(player, event.getBlockClicked()))
      event.setCancelled(true);
  }

  private boolean handleInteractionSessionAndGetIfCancel(Player player, Block target) {
    var playerId = player.getUniqueId();

    var lastCancelStamp = lastEventCancelTimeByPlayerId.getLong(playerId);

    // Some events, like interact/block-break, may call multiple times, and if we're not in
    // multi-use mode, the session will have been removed at this point; also, avoid spamming
    // interactions which have only been initiated once by a single click. Some blocks like
    // grass, mushrooms, etc. will fire multiple times (up to 3-4 ticks delayed!) - also save
    // those from being destroyed accidentally.
    if (lastCancelStamp >= 0 && relativeTime - lastCancelStamp <= 5)
      return true;

    var interactionSession = interactionSessionByPlayerId.get(playerId);

    if (interactionSession == null)
      return false;

    if (!interactionSession.allowMultiUse)
      interactionSessionByPlayerId.remove(player.getUniqueId());

    interactionSession.interactionHandler.accept(target);
    interactionSession.touchExpiry();

    lastEventCancelTimeByPlayerId.put(playerId, relativeTime);
    return true;
  }
}

package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ConfigKeeperReloadEvent;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.PlaytimeRewardsAPI;
import at.blvckbytes.playtime_rewards.store.TimeType;
import at.blvckbytes.playtime_rewards.store.TopListDirection;
import at.blvckbytes.playtime_rewards.store.TopListType;
import com.gamingmesh.jobs.Jobs;
import com.gmail.nossr50.util.player.UserManager;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_tool.AutoToolCommand;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterCommand;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import me.blvckbytes.bbtweaks.multi_break.BlockDirections;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.bbtweaks.integration.arm.ArmIntegration;
import me.blvckbytes.bbtweaks.sidebar.preferences.DelimitersMode;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesStore;
import me.blvckbytes.bbtweaks.sidebar.preferences.SneakMode;
import me.blvckbytes.bbtweaks.util.*;
import net.ess3.api.IEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SidebarBoardManager implements Listener, Tickable, StatisticEnvironmentResolver {

  private static final long TICKS_AT_MIDNIGHT = 18000;
  private static final long TICKS_PER_DAY = 24000;
  private static final long TICKS_PER_HOUR = TICKS_PER_DAY / 24;
  private static final double TICKS_PER_MINUTE = TICKS_PER_HOUR / 60d;
  private static final double TICKS_PER_SECOND = TICKS_PER_MINUTE / 60d;

  private final Plugin plugin;
  private final MultiBreakParametersStore multiBreakParametersStore;
  private final InvMagnetParametersStore invMagnetParametersStore;
  private final InvFilterCommand invFilterCommand;
  private final AutoToolCommand autoToolCommand;
  private final ArmIntegration armIntegration;
  private final FloodgateIntegration floodgateIntegration;
  private final SidebarPreferencesStore sidebarPreferencesStore;
  private final AutoPickupContainerListener autoPickupContainerListener;
  private final PlaytimeRewardsAPI playtimeRewards;
  private final LuckPerms luckPerms;
  private final IEssentials essentials;

  private final ConfigKeeper<MainSection> config;
  private final Map<UUID, SidebarBoard> boardByPlayerId;
  private final Map<UUID, Long> lastSneakStampByPlayerId;

  private final boolean hasJobs, hasMcMMO;

  private long relativeTime;

  public SidebarBoardManager(
    Plugin plugin,
    MultiBreakParametersStore multiBreakParametersStore,
    InvMagnetParametersStore invMagnetParametersStore,
    InvFilterCommand invFilterCommand,
    AutoToolCommand autoToolCommand,
    ArmIntegration armIntegration,
    FloodgateIntegration floodgateIntegration,
    SidebarPreferencesStore sidebarPreferencesStore,
    AutoPickupContainerListener autoPickupContainerListener,
    ConfigKeeper<MainSection> config
  ) {
    this.multiBreakParametersStore = multiBreakParametersStore;
    this.invMagnetParametersStore = invMagnetParametersStore;
    this.invFilterCommand = invFilterCommand;
    this.autoToolCommand = autoToolCommand;
    this.armIntegration = armIntegration;
    this.floodgateIntegration = floodgateIntegration;
    this.sidebarPreferencesStore = sidebarPreferencesStore;
    this.autoPickupContainerListener = autoPickupContainerListener;

    var playtimeRegistration = Bukkit.getServicesManager().getRegistration(PlaytimeRewardsAPI.class);

    if (playtimeRegistration == null)
      throw new IllegalStateException("Could not locate registration for the playtime API");

    var luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

    if (luckPermsProvider == null)
      throw new IllegalStateException("Could not locate registration for the LuckPerms API");

    this.luckPerms = luckPermsProvider.getProvider();

    this.essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

    if (essentials == null)
      throw new IllegalStateException("Expected Essentials to be loaded");

    this.hasJobs = Bukkit.getPluginManager().isPluginEnabled("Jobs");
    this.hasMcMMO = Bukkit.getPluginManager().isPluginEnabled("mcMMO");

    this.playtimeRewards = playtimeRegistration.getProvider();

    this.plugin = plugin;
    this.config = config;

    this.boardByPlayerId = new HashMap<>();
    this.lastSneakStampByPlayerId = new HashMap<>();
  }

  @Override
  public void tick(long relativeTime) {
    this.relativeTime = relativeTime;

    if (relativeTime % config.rootSection.sidebar.updateIntervalTicks != 0)
      return;

    for (var board : boardByPlayerId.values()) {
      var preferences = sidebarPreferencesStore.accessPreferences(board.holder.bukkitPlayer());
      var isSneaking = board.holder.bukkitPlayer().isSneaking();

      if (
        !preferences.enabled
          || (isSneaking && preferences.sneakMode == SneakMode.DISABLE_DURING_SNEAK)
          || (!isSneaking && preferences.sneakMode == SneakMode.ENABLE_DURING_SNEAK)
      ) {
        board.unregisterIfShown();
        continue;
      }

      renderAndUpdateLinesForBoard(board, preferences);
    }
  }

  @EventHandler
  public void onConfigReload(ConfigKeeperReloadEvent event) {
    if (event.configKeeper != config)
      return;

    // Cause a re-build by unregistering, such that it re-registers on next update.
    for (var board : boardByPlayerId.values())
      board.unregisterIfShown();
  }

  @EventHandler
  public void onSneakToggle(PlayerToggleSneakEvent event) {
    if (!event.isSneaking())
      return;

    var player = event.getPlayer();
    var preferences = sidebarPreferencesStore.accessPreferences(player);

    if (preferences.sneakMode == SneakMode.DISABLE_DURING_SNEAK) {
      var board = boardByPlayerId.get(player.getUniqueId());

      if (board != null)
        board.unregisterIfShown();

      return;
    }

    if (preferences.sneakMode == SneakMode.ENABLE_DURING_SNEAK) {
      var board = boardByPlayerId.get(player.getUniqueId());

      if (board != null)
        renderAndUpdateLinesForBoard(board, preferences);

      return;
    }

    if (preferences.sneakMode == SneakMode.DOUBLE_SNEAK_TOGGLES) {
      var now = System.currentTimeMillis();
      var playerId = player.getUniqueId();

      var lastSneakStamp = lastSneakStampByPlayerId.get(playerId);

      lastSneakStampByPlayerId.put(playerId, now);

      if (lastSneakStamp == null)
        return;

      if (now - lastSneakStamp > config.rootSection.sidebar.doubleSneakMaxDelayMs)
        return;

      preferences.toggleEnabled();
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var essentialsUser = essentials.getUser(player);

      if (essentialsUser == null) {
        plugin.getLogger().severe("Could not access essentials-user for " + player.getUniqueId() + " (" + player.getName() + ")");
        return;
      }

      var holder = new BoardHolder(player, essentialsUser, floodgateIntegration.isFloodgatePlayer(player));

      var board = new SidebarBoard(plugin, holder, config);

      boardByPlayerId.put(player.getUniqueId(), board);
    }, 1);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    boardByPlayerId.remove(event.getPlayer().getUniqueId());
    lastSneakStampByPlayerId.remove(event.getPlayer().getUniqueId());
  }

  private void renderAndUpdateLinesForBoard(SidebarBoard board, SidebarPreferences preferences) {
    var maxLineCount = SidebarStatistic.ALL_VALUES.size();
    var staticLineIndices = IntSet.of();

    switch (preferences.delimitersMode) {
      case NONE -> {}
      case TOP_ONLY -> {
        ++maxLineCount;
        staticLineIndices = new IntArraySet(1);
      }
      case TOP_AND_BOTTOM -> {
        maxLineCount += 2;
        staticLineIndices = new IntArraySet(2);
      }
      default -> throw new IllegalStateException("Unaccounted-for delimiters-mode: " + preferences.delimitersMode);
    }

    var lines = new ArrayList<Component>(maxLineCount);

    if (preferences.delimitersMode != DelimitersMode.NONE) {
      staticLineIndices.add(0);
      lines.add(Component.empty());
    }

    var lengthBuffer = new MutableInt();
    var maxLineLength = 0;

    for (var statistic : preferences.statisticsInOrder) {
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);
      var line = statistic.renderFor(board.holder, statisticSection, preferences, this);

      if (line == null)
        continue;

      lengthBuffer.value = 0;

      ComponentUtil.forEachTextOfComponent(line, text -> lengthBuffer.value += text.length());

      if (lengthBuffer.value > maxLineLength)
        maxLineLength = lengthBuffer.value;

      lines.add(line);
    }

    if (preferences.delimitersMode != DelimitersMode.NONE) {
      var delimiter = config.rootSection.sidebar.delimiter.interpret(
        SlotType.SINGLE_LINE_CHAT,
        new InterpretationEnvironment()
          .withVariable("is_floodgate", board.holder.isFloodgate())
          .withVariable("max_line_length", maxLineLength)
      ).getFirst();

      lines.set(0, delimiter);

      if (preferences.delimitersMode == DelimitersMode.TOP_AND_BOTTOM) {
        staticLineIndices.add(lines.size());
        lines.add(delimiter);
      }
    }

    board.advanceScrollingAndSetLines(relativeTime, lines, staticLineIndices, preferences);
  }

  @Override
  public InterpretationEnvironment resolve(BoardHolder holder, SidebarStatistic statistic) {
    var player = holder.bukkitPlayer();

    var environment = new InterpretationEnvironment()
      .withVariable("is_floodgate", holder.isFloodgate());

    switch (statistic) {
      case GROUP_PREFIX -> {
        var prefix = luckPerms.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();

        if (prefix == null)
          return environment.withVariable("prefix", "?");

        return environment
          .withVariable("prefix", LegacyComponentSerializer.legacySection().deserialize(LegacyColorUtil.enableColors(prefix.trim())));
      }

      case MONEY -> {
        return environment
          .withVariable("balance", holder.essentialsUser().getMoney().doubleValue());
      }

      case TOTAL_PLAYTIME -> {
        return environment
          .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.PLAY_TIME));
      }

      case TOTAL_PLAYTIME_TOP_PLACE -> {
        return environment
          .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.PLAY_TIME));
      }

      case TOTAL_AFKTIME -> {
        return environment
          .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.AFK_TIME));
      }

      case TOTAL_AFKTIME_TOP_PLACE -> {
        return environment
          .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.AFK_TIME));
      }

      case HOME_COUNT -> {
        return environment
          .withVariable("current_home_count", holder.essentialsUser().getHomes().size())
          .withVariable("total_home_count", essentials.getSettings().getHomeLimit(holder.essentialsUser()));
      }

      case PING -> {
        return environment
          .withVariable("ping", player.getPing());
      }

      case DATE, REAL_TIME -> {
        return environment
          .withVariable("millis", System.currentTimeMillis());
      }

      case COORDINATES -> {
        return environment
          .withVariable("x", (int) player.getX())
          .withVariable("y", (int) player.getY())
          .withVariable("z", (int) player.getZ())
          .withVariable("world", player.getWorld().getName());
      }

      case BIOME -> {
        var location = player.getLocation();
        var biome = player.getWorld().getBiome(location);

        return environment
          .withVariable("biome_key", biome.translationKey());
      }

      case LOOKING_DIRECTION -> {
        var face = BlockDirections.directionToBlockFace(player.getLocation().getDirection());

        return environment
          .withVariable("direction", StringUtils.capitalize(face.name().toLowerCase()));
      }

      case GAME_TIME -> {
        double time = player.getWorld().getTime() - TICKS_AT_MIDNIGHT + TICKS_PER_DAY;

        int hours = 0, minutes = 0, seconds = 0;

        while (time >= TICKS_PER_DAY)
          time -= TICKS_PER_DAY;

        while (time >= TICKS_PER_HOUR) {
          ++hours;
          time -= TICKS_PER_HOUR;
        }

        while (time >= TICKS_PER_MINUTE) {
          ++minutes;
          time -= TICKS_PER_MINUTE;
        }

        while (time > 0) {
          ++seconds;
          time -= TICKS_PER_SECOND;
        }

        return environment
          .withVariable("hours", hours)
          .withVariable("minutes", minutes)
          .withVariable("seconds", seconds);
      }

      case FIRST_JOB_PROGRESSION -> {
        if (!hasJobs)
          return environment.withVariable("name", null).withVariable("progression", null);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.isEmpty())
          return environment.withVariable("name", null).withVariable("progression", null);

        var progression = progressions.getFirst();

        return environment
          .withVariable("name", progression.getJob().getName())
          .withVariable("progression", JobProgressionData.fromProgression(progression));
      }

      case SECOND_JOB_PROGRESSION -> {
        if (!hasJobs)
          return environment.withVariable("name", null).withVariable("progression", null);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.size() < 2)
          return environment.withVariable("name", null).withVariable("progression", null);

        var progression = progressions.get(1);

        return environment
          .withVariable("name", progression.getJob().getName())
          .withVariable("progression", JobProgressionData.fromProgression(progression));
      }

      case MCMMO_POWER_LEVEL -> {
        if (!hasMcMMO)
          return environment.withVariable("power_level", "?");

        var user = UserManager.getPlayer(player);

        if (user == null)
          return environment.withVariable("power_level", "?");

        return environment
          .withVariable("power_level", user.getPowerLevel());
      }

      case PLAYER_NAME -> {
        return environment
          .withVariable("name", player.getName());
      }

      case TPS -> {
        return environment
          .withVariable("tps", Bukkit.getServer().getTPS());
      }

      case LIGHT_LEVEL -> {
        var result = player.rayTraceBlocks(5);

        Block block;
        BlockFace face;

        if (result == null || (block = result.getHitBlock()) == null || (face = result.getHitBlockFace()) == null)
          return environment.withVariable("has_block", false);

        if (block.getType().isOccluding())
          block = block.getRelative(face);

        return environment
          .withVariable("has_block", true)
          .withVariable("light_sky", block.getLightFromSky())
          .withVariable("light_blocks", block.getLightFromBlocks());
      }

      case MULTIBREAK_STATUS -> {
        var parametersSlots = multiBreakParametersStore.accessParametersSlots(player);

        return environment
          .withVariable("enabled", parametersSlots.isEnabledAndInAllowedWorld())
          .withVariable("slot_index", parametersSlots.getSelectedSlotIndex());
      }

      case INV_MAGNET_STATUS -> {
        var parameters = invMagnetParametersStore.accessParameters(player);
        var isInAllowedWorld = invMagnetParametersStore.getAllowedWorlds().contains(player.getWorld());

        return environment
          .withVariable("enabled", isInAllowedWorld && parameters.enabled)
          .withVariable("radius", parameters.getRadius())
          .withVariable("max_radius", parameters.getLimits().maxRadius());
      }

      case INV_FILTER_STATUS -> {
        return environment
          .withVariable("enabled", invFilterCommand.getOrLoadFilter(player).enabled);
      }

      case AUTOTOOL_STATUS -> {
        return environment
          .withVariable("enabled", autoToolCommand.isEnabled(player));
      }

      case CURRENT_AFK_DURATION -> {
        return environment
          .withVariable(
            "time",
            holder.essentialsUser().isAfk()
              ? System.currentTimeMillis() - holder.essentialsUser().getAfkSince()
              : null
          );
      }

      case REMAINING_PLAYTIME_UNTIL_NEXT_RANK -> {
        return environment
          .withVariable("time", playtimeRewards.getRemainingTimeUntilNextRank(player));
      }

      case REMAINING_SHOP_REGION_RENT_DURATION -> {
        return environment
          .withVariable("time", armIntegration.getRemainingShopRegionTime(player));
      }

      case REMAINING_CREATIVE_REGION_RENT_DURATION -> {
        return environment
          .withVariable("time", armIntegration.getRemainingCreativeRegionTime(player));
      }

      case AUTO_PICKUP_CONTAINER_USAGE_ABSOLUTE, AUTO_PICKUP_CONTAINER_USAGE_RELATIVE -> {
        var usageCounts = autoPickupContainerListener.getLastKnownUsageCounts(player);

        return environment
          .withVariable("used_slots", usageCounts.usedSlots())
          .withVariable("vacant_slots", usageCounts.vacantSlots());
      }
    }

    return environment;
  }
}

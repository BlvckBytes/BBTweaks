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
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_pickup_container.AutoPickupContainerListener;
import me.blvckbytes.bbtweaks.auto_tool.AutoToolCommand;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import me.blvckbytes.bbtweaks.block_facing.settings.BlockFacingSettingsStore;
import me.blvckbytes.bbtweaks.hotbar_randomizer.HotbarRandomizerSettingsStore;
import me.blvckbytes.bbtweaks.integration.floodgate.FloodgateIntegration;
import me.blvckbytes.bbtweaks.inv_filter.InvFilterProfileStore;
import me.blvckbytes.bbtweaks.inv_magnet.parameters.InvMagnetParametersStore;
import me.blvckbytes.bbtweaks.multi_break.BlockDirections;
import me.blvckbytes.bbtweaks.multi_break.parameters.MultiBreakParametersStore;
import me.blvckbytes.bbtweaks.integration.arm.ArmIntegration;
import me.blvckbytes.bbtweaks.sidebar.preferences.*;
import me.blvckbytes.bbtweaks.util.*;
import net.ess3.api.IEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
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
  private final InvFilterProfileStore invFilterProfileStore;
  private final AutoToolCommand autoToolCommand;
  private final ArmIntegration armIntegration;
  private final FloodgateIntegration floodgateIntegration;
  private final SidebarPreferencesStore sidebarPreferencesStore;
  private final AutoPickupContainerListener autoPickupContainerListener;
  private final BlockFacingSettingsStore blockFacingSettingsStore;
  private final HotbarRandomizerSettingsStore hotbarRandomizerSettingsStore;
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
    InvFilterProfileStore invFilterProfileStore,
    AutoToolCommand autoToolCommand,
    ArmIntegration armIntegration,
    FloodgateIntegration floodgateIntegration,
    SidebarPreferencesStore sidebarPreferencesStore,
    AutoPickupContainerListener autoPickupContainerListener,
    BlockFacingSettingsStore blockFacingSettingsStore,
    HotbarRandomizerSettingsStore hotbarRandomizerSettingsStore,
    ConfigKeeper<MainSection> config
  ) {
    this.multiBreakParametersStore = multiBreakParametersStore;
    this.invMagnetParametersStore = invMagnetParametersStore;
    this.invFilterProfileStore = invFilterProfileStore;
    this.autoToolCommand = autoToolCommand;
    this.armIntegration = armIntegration;
    this.floodgateIntegration = floodgateIntegration;
    this.sidebarPreferencesStore = sidebarPreferencesStore;
    this.autoPickupContainerListener = autoPickupContainerListener;
    this.blockFacingSettingsStore = blockFacingSettingsStore;
    this.hotbarRandomizerSettingsStore = hotbarRandomizerSettingsStore;

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
      var preferencesSlots = sidebarPreferencesStore.accessPreferencesSlots(board.holder.bukkitPlayer());
      var preferences = preferencesSlots.getSelectedPreferences();

      var isSneaking = board.holder.bukkitPlayer().isSneaking();

      if (!preferencesSlots.enabled) {
        board.unregisterIfShown();
        continue;
      }

      if ((isSneaking && preferences.sneakMode == SneakMode.DISABLE_DURING_SNEAK)
        || (!isSneaking && preferences.sneakMode == SneakMode.ENABLE_DURING_SNEAK)) {
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
    var preferencesSlots = sidebarPreferencesStore.accessPreferencesSlots(player);
    var preferences = preferencesSlots.getSelectedPreferences();

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

      preferencesSlots.setEnabled(null);
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

    var renderedLines = new ArrayList<RenderedLine>(maxLineCount);

    if (preferences.delimitersMode != DelimitersMode.NONE) {
      staticLineIndices.add(0);
      renderedLines.add(new RenderedLine(Component.empty(), null, 0));
    }

    var lengthBuffer = new MutableInt();
    var maxLineLength = 0;

    for (var statistic : preferences.statisticsInOrder) {
      var statisticSection = config.rootSection.sidebar._statisticsMap.get(statistic);
      var renderedLine = statistic.renderFor(board.holder, statisticSection, preferences, this);

      if (renderedLine == null)
        continue;

      lengthBuffer.value = 0;

      ComponentUtil.forEachTextOfComponent(renderedLine.component(), text -> lengthBuffer.value += text.length());

      if (lengthBuffer.value > maxLineLength)
        maxLineLength = lengthBuffer.value;

      renderedLines.add(renderedLine);
    }

    if (preferences.delimitersMode != DelimitersMode.NONE) {
      var delimiter = config.rootSection.sidebar.delimiter.interpret(
        SlotType.SINGLE_LINE_CHAT,
        new InterpretationEnvironment()
          .withVariable("is_floodgate", board.holder.isFloodgate())
          .withVariable("max_line_length", maxLineLength)
      ).getFirst();

      renderedLines.set(0, new RenderedLine(delimiter, null, 0));

      if (preferences.delimitersMode == DelimitersMode.TOP_AND_BOTTOM) {
        staticLineIndices.add(renderedLines.size());
        renderedLines.add(new RenderedLine(delimiter, null, 0));
      }
    }

    var lines = sortAndUnwrapRenderedLines(renderedLines, preferences.autoSortMode);

    board.advanceScrollingAndSetLines(relativeTime, lines, staticLineIndices, preferences);
  }

  private List<Component> sortAndUnwrapRenderedLines(List<RenderedLine> renderedLines, AutoSortMode autoSortMode) {
    var result = new ArrayList<Component>(renderedLines.size());

    if (autoSortMode != AutoSortMode.OFF) {
      var groupMemberLines = new ArrayList<RenderedLine>(renderedLines.size());
      var affectedStatistics = EnumSet.noneOf(SidebarStatistic.class);

      for (var sortingGroup : SidebarSortingGroup.ALL_VALUES) {
        groupMemberLines.clear();
        affectedStatistics.clear();

        for (var line : renderedLines) {
          if (line.statistic() == null || !sortingGroup.members.contains(line.statistic()))
            continue;

          groupMemberLines.add(line);
          affectedStatistics.add(line.statistic());
        }

        if (groupMemberLines.isEmpty())
          continue;

        groupMemberLines.sort((a, b) -> {
          return Integer.compare(a.sortingValue(), b.sortingValue()) * (autoSortMode == AutoSortMode.ASCENDING ? 1 : -1);
        });

        var nextGroupMemberIndex = 0;

        for (var index = 0; index < renderedLines.size(); ++index) {
          var currentLine = renderedLines.get(index);

          if (!affectedStatistics.contains(currentLine.statistic()))
            continue;

          var currentGroupMemberIndex = nextGroupMemberIndex++;

          if (currentGroupMemberIndex >= groupMemberLines.size())
            break;

          renderedLines.set(index, groupMemberLines.get(currentGroupMemberIndex));
        }
      }
    }

    for (var renderedLine : renderedLines)
      result.add(renderedLine.component());

    return result;
  }

  @Override
  public EnvironmentAndSortingValue resolve(BoardHolder holder, SidebarStatistic statistic) {
    var player = holder.bukkitPlayer();

    var environment = new InterpretationEnvironment()
      .withVariable("is_floodgate", holder.isFloodgate());

    switch (statistic) {
      case GROUP_PREFIX -> {
        var prefix = luckPerms.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();

        if (prefix == null)
          return new EnvironmentAndSortingValue(environment.withVariable("prefix", "?"), 0);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("prefix", LegacyComponentSerializer.legacySection().deserialize(LegacyColorUtil.enableColors(prefix.trim()))),
          0
        );
      }

      case MONEY -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("balance", holder.essentialsUser().getMoney().doubleValue()),
          0
        );
      }

      case TOTAL_PLAYTIME -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.PLAY_TIME)),
          0
        );
      }

      case TOTAL_PLAYTIME_TOP_PLACE -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.PLAY_TIME)),
          0
        );
      }

      case TOTAL_AFKTIME -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.AFK_TIME)),
          0
        );
      }

      case TOTAL_AFKTIME_TOP_PLACE -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.AFK_TIME)),
          0
        );
      }

      case HOME_COUNT -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("current_home_count", holder.essentialsUser().getHomes().size())
            .withVariable("total_home_count", essentials.getSettings().getHomeLimit(holder.essentialsUser())),
          0
        );
      }

      case PING -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("ping", player.getPing()),
          0
        );
      }

      case DATE, REAL_TIME -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("millis", System.currentTimeMillis()),
          0
        );
      }

      case COORDINATES -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("x", (int) player.getX())
            .withVariable("y", (int) player.getY())
            .withVariable("z", (int) player.getZ())
            .withVariable("world", player.getWorld().getName()),
          0
        );
      }

      case BIOME -> {
        var location = player.getLocation();
        var biome = player.getWorld().getBiome(location);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("biome_key", biome.translationKey()),
          0
        );
      }

      case LOOKING_DIRECTION -> {
        var face = BlockDirections.directionToBlockFace(player.getLocation().getDirection());

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("direction", StringUtils.capitalize(face.name().toLowerCase())),
          0
        );
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

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("hours", hours)
            .withVariable("minutes", minutes)
            .withVariable("seconds", seconds),
          0
        );
      }

      case FIRST_JOB_PROGRESSION -> {
        if (!hasJobs)
          return new EnvironmentAndSortingValue(environment.withVariable("name", null).withVariable("progression", null), 0);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.isEmpty())
          return new EnvironmentAndSortingValue(environment.withVariable("name", null).withVariable("progression", null), 0);

        var progression = progressions.getFirst();

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("name", progression.getJob().getName())
            .withVariable("progression", JobProgressionData.fromProgression(progression)),
          0
        );
      }

      case SECOND_JOB_PROGRESSION -> {
        if (!hasJobs)
          return new EnvironmentAndSortingValue(environment.withVariable("name", null).withVariable("progression", null), 0);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.size() < 2)
          return new EnvironmentAndSortingValue(environment.withVariable("name", null).withVariable("progression", null), 0);

        var progression = progressions.get(1);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("name", progression.getJob().getName())
            .withVariable("progression", JobProgressionData.fromProgression(progression)),
          0
        );
      }

      case MCMMO_POWER_LEVEL -> {
        if (!hasMcMMO)
          return new EnvironmentAndSortingValue(environment.withVariable("power_level", "?"), 0);

        var user = UserManager.getPlayer(player);

        if (user == null)
          return new EnvironmentAndSortingValue(environment.withVariable("power_level", "?"), 0);

        return new EnvironmentAndSortingValue(
          environment.withVariable("power_level", user.getPowerLevel()),
          0
        );
      }

      case PLAYER_NAME -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("name", player.getName()),
          0
        );
      }

      case TPS -> {
        var tickTimes = Bukkit.getTickTimes();
        double tickMillisSum = 0;

        for (var tickTimeNano : tickTimes)
          tickMillisSum += tickTimeNano / 1000.0 / 1000.0;

        var tickMillisAverage = tickTimes.length == 0 ? 0 : tickMillisSum / tickTimes.length;

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("tps", Bukkit.getServer().getTPS())
            .withVariable("average_mspt", tickMillisAverage),
          0
        );
      }

      case LIGHT_LEVEL -> {
        var result = player.rayTraceBlocks(5);

        Block block;
        BlockFace face;

        if (result == null || (block = result.getHitBlock()) == null || (face = result.getHitBlockFace()) == null)
          return new EnvironmentAndSortingValue(environment.withVariable("has_block", false), 0);

        if (block.getType().isOccluding())
          block = block.getRelative(face);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("has_block", true)
            .withVariable("light_sky", block.getLightFromSky())
            .withVariable("light_blocks", block.getLightFromBlocks()),
          0
        );
      }

      case MULTIBREAK_STATUS -> {
        var parametersSlots = multiBreakParametersStore.accessParametersSlots(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", parametersSlots.isEnabledAndInAllowedWorld())
            .withVariable("slot_index", parametersSlots.getSelectedSlotIndex()),
          0
        );
      }

      case INV_MAGNET_STATUS -> {
        var parameters = invMagnetParametersStore.accessParameters(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", parameters.isEnabled())
            .withVariable("radius", parameters.getRadius())
            .withVariable("max_radius", parameters.getLimits().maxRadius()),
          0
        );
      }

      case INV_FILTER_STATUS -> {
        var profile = invFilterProfileStore.access(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", profile.enabled)
            .withVariable("slot", profile.getSelectedSlotIndex() + 1),
          0
        );
      }

      case AUTOTOOL_STATUS -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", autoToolCommand.isEnabled(player)),
          0
        );
      }

      case CURRENT_AFK_DURATION -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable(
              "time",
              holder.essentialsUser().isAfk()
                ? System.currentTimeMillis() - holder.essentialsUser().getAfkSince()
                : null
            ),
          0
        );
      }

      case REMAINING_PLAYTIME_UNTIL_NEXT_RANK -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("time", playtimeRewards.getRemainingTimeUntilNextRank(player)),
          0
        );
      }

      case REMAINING_SHOP_REGION_RENT_DURATION -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("time", armIntegration.getRemainingShopRegionTime(player)),
          0
        );
      }

      case REMAINING_CREATIVE_REGION_RENT_DURATION -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("time", armIntegration.getRemainingCreativeRegionTime(player)),
          0
        );
      }

      case AUTO_PICKUP_CONTAINER_USAGE_ABSOLUTE, AUTO_PICKUP_CONTAINER_USAGE_RELATIVE -> {
        var usageCounts = autoPickupContainerListener.getLastKnownUsageCounts(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("used_slots", usageCounts.usedSlots())
            .withVariable("vacant_slots", usageCounts.vacantSlots())
            .withVariable("container_count", usageCounts.containerCount()),
          0
        );
      }

      case BLOCK_FACING_STATUS -> {
        var settings = blockFacingSettingsStore.access(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", settings.enabled)
            .withVariable("facing", settings.facingOverride.sidebarShorthand),
          0
        );
      }

      case HOTBAR_RANDOMIZER_STATUS -> {
        var settings = hotbarRandomizerSettingsStore.accessSettings(player);

        return new EnvironmentAndSortingValue(
          environment
            .withVariable("enabled", settings.enabled),
          0
        );
      }

      case PLAYER_COUNT -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("player_count", Bukkit.getOnlinePlayers().size()),
          0
        );
      }

      case DEATH_COUNT -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("death_count", player.getStatistic(Statistic.DEATHS)),
          0
        );
      }

      case WORLD_NAME -> {
        return new EnvironmentAndSortingValue(
          environment
            .withVariable("world_name", player.getWorld().getName().toLowerCase()),
          0
        );
      }
    }

    if (statistic.ordinal() >= SidebarStatistic.MCMMO_ACROBATICS_LEVEL.ordinal() && statistic.ordinal() <= SidebarStatistic.MCMMO_WOODCUTTING_LEVEL.ordinal()) {
      if (!hasMcMMO)
        return new EnvironmentAndSortingValue(environment.withVariable("skill_level", "?").withVariable("skill_name", "?"), 0);

      var skillType = switch (statistic) {
        case MCMMO_ACROBATICS_LEVEL -> PrimarySkillType.ACROBATICS;
        case MCMMO_ALCHEMY_LEVEL -> PrimarySkillType.ALCHEMY;
        case MCMMO_ARCHERY_LEVEL -> PrimarySkillType.ARCHERY;
        case MCMMO_AXES_LEVEL -> PrimarySkillType.AXES;
        case MCMMO_CROSSBOWS_LEVEL -> PrimarySkillType.CROSSBOWS;
        case MCMMO_EXCAVATION_LEVEL -> PrimarySkillType.EXCAVATION;
        case MCMMO_FISHING_LEVEL -> PrimarySkillType.FISHING;
        case MCMMO_HERBALISM_LEVEL -> PrimarySkillType.HERBALISM;
        case MCMMO_MACES_LEVEL -> PrimarySkillType.MACES;
        case MCMMO_MINING_LEVEL -> PrimarySkillType.MINING;
        case MCMMO_REPAIR_LEVEL -> PrimarySkillType.REPAIR;
        case MCMMO_SALVAGE_LEVEL -> PrimarySkillType.SALVAGE;
        case MCMMO_SMELTING_LEVEL -> PrimarySkillType.SMELTING;
        case MCMMO_SPEARS_LEVEL -> PrimarySkillType.SPEARS;
        case MCMMO_SWORDS_LEVEL -> PrimarySkillType.SWORDS;
        case MCMMO_TAMING_LEVEL -> PrimarySkillType.TAMING;
        case MCMMO_TRIDENTS_LEVEL -> PrimarySkillType.TRIDENTS;
        case MCMMO_UNARMED_LEVEL -> PrimarySkillType.UNARMED;
        case MCMMO_WOODCUTTING_LEVEL -> PrimarySkillType.WOODCUTTING;
        default -> null;
      };

      if (skillType == null)
        return new EnvironmentAndSortingValue(environment.withVariable("skill_level", "?").withVariable("skill_name", "?"), 0);

      var user = UserManager.getPlayer(player);
      var level = user == null ? null : user.getSkillLevel(skillType);

      return new EnvironmentAndSortingValue(
        environment
          .withVariable("skill_level", level == null ? "?" : level)
          .withVariable("skill_name", mcMMO.p.getSkillTools().getLocalizedSkillName(skillType)),
        level == null ? 0 : level
      );
    }

    return new EnvironmentAndSortingValue(environment, 0);
  }
}

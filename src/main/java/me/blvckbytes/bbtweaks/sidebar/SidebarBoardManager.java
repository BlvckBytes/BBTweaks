package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.PlaytimeRewardsAPI;
import at.blvckbytes.playtime_rewards.store.TimeType;
import at.blvckbytes.playtime_rewards.store.TopListDirection;
import at.blvckbytes.playtime_rewards.store.TopListType;
import com.gamingmesh.jobs.Jobs;
import com.gmail.nossr50.util.player.UserManager;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.multi_break.BlockDirections;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferencesStore;
import me.blvckbytes.bbtweaks.sidebar.preferences.SneakMode;
import net.ess3.api.IEssentials;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SidebarBoardManager implements Listener, StatisticEnvironmentResolver {

  private static final long TICKS_AT_MIDNIGHT = 18000;
  private static final long TICKS_PER_DAY = 24000;
  private static final long TICKS_PER_HOUR = TICKS_PER_DAY / 24;
  private static final double TICKS_PER_MINUTE = TICKS_PER_HOUR / 60d;
  private static final double TICKS_PER_SECOND = TICKS_PER_MINUTE / 60d;

  private final Plugin plugin;
  private final SidebarPreferencesStore sidebarPreferencesStore;
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
    SidebarPreferencesStore sidebarPreferencesStore,
    ConfigKeeper<MainSection> config
  ) {
    this.sidebarPreferencesStore = sidebarPreferencesStore;

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

    // Cause a re-build by unregistering, such that it re-registers on next update.
    config.registerReloadListener(() -> {
      for (var board : boardByPlayerId.values())
        board.unregisterIfShown();
    });

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ++relativeTime;

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
    }, 0, 0);
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

      var holder = new BoardHolder(player, essentialsUser);

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
    var lines = preferences.statisticsInOrder.stream()
      .filter(preferences.enabledStatistics::contains)
      .map(it -> it.renderFor(board.holder, config.rootSection.sidebar._statisticsMap.get(it), preferences, this))
      .toList();

    board.advanceScrollingAndSetLines(relativeTime, lines, preferences);
  }

  @Override
  public InterpretationEnvironment resolve(BoardHolder holder, SidebarStatistic statistic) {
    var player = holder.bukkitPlayer();

    switch (statistic) {
      case GROUP_PREFIX -> {
        var prefix = luckPerms.getPlayerAdapter(Player.class).getMetaData(player).getPrefix();

        if (prefix == null)
          return new InterpretationEnvironment().withVariable("prefix", "?");

        return new InterpretationEnvironment()
          .withVariable("prefix", LegacyComponentSerializer.legacySection().deserialize(enableColors(prefix.trim())));
      }

      case MONEY -> {
        return new InterpretationEnvironment()
          .withVariable("balance", holder.essentialsUser().getMoney().doubleValue());
      }

      case TOTAL_PLAYTIME -> {
        return new InterpretationEnvironment()
          .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.PLAY_TIME));
      }

      case TOTAL_PLAYTIME_TOP_PLACE -> {
        return new InterpretationEnvironment()
          .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.PLAY_TIME));
      }

      case TOTAL_AFKTIME -> {
        return new InterpretationEnvironment()
          .withVariable("time", playtimeRewards.getTotalTimeTicks(player, TimeType.AFK_TIME));
      }

      case TOTAL_AFKTIME_TOP_PLACE -> {
        return new InterpretationEnvironment()
          .withVariable("place", playtimeRewards.getTopListNumber(player, TopListType.TOTAL, TopListDirection.DESCENDING, TimeType.AFK_TIME));
      }

      case HOME_COUNT -> {
        return new InterpretationEnvironment()
          .withVariable("current_home_count", holder.essentialsUser().getHomes().size())
          .withVariable("total_home_count", essentials.getSettings().getHomeLimit(holder.essentialsUser()));
      }

      case PING -> {
        return new InterpretationEnvironment()
          .withVariable("ping", player.getPing());
      }

      case DATE, REAL_TIME -> {
        return new InterpretationEnvironment()
          .withVariable("millis", System.currentTimeMillis());
      }

      case COORDINATES -> {
        return new InterpretationEnvironment()
          .withVariable("x", (int) player.getX())
          .withVariable("y", (int) player.getY())
          .withVariable("z", (int) player.getZ())
          .withVariable("world", player.getWorld().getName());
      }

      case BIOME -> {
        var location = player.getLocation();
        var biome = player.getWorld().getBiome(location);

        return new InterpretationEnvironment()
          .withVariable("biome_key", biome.translationKey());
      }

      case LOOKING_DIRECTION -> {
        var face = BlockDirections.directionToBlockFace(player.getLocation().getDirection());

        return new InterpretationEnvironment()
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

        return new InterpretationEnvironment()
          .withVariable("hours", hours)
          .withVariable("minutes", minutes)
          .withVariable("seconds", seconds);
      }

      case FIRST_JOB_PROGRESSION -> {
        if (!hasJobs)
          return new InterpretationEnvironment().withVariable("progression", null);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.isEmpty())
          return new InterpretationEnvironment().withVariable("progression", null);

        var progression = progressions.getFirst();

        return new InterpretationEnvironment()
          .withVariable("name", progression.getJob().getName())
          .withVariable("progression", JobProgressionData.fromProgression(progression));
      }

      case SECOND_JOB_PROGRESSION -> {
        if (!hasJobs)
          return new InterpretationEnvironment().withVariable("progression", null);

        var progressions = Jobs.getPlayerManager().getJobsPlayer(player).getJobProgression();

        if (progressions.size() < 2)
          return new InterpretationEnvironment().withVariable("progression", null);

        var progression = progressions.get(1);

        return new InterpretationEnvironment()
          .withVariable("name", progression.getJob().getName())
          .withVariable("progression", JobProgressionData.fromProgression(progression));
      }

      case MCMMO_POWER_LEVEL -> {
        if (!hasMcMMO)
          return new InterpretationEnvironment().withVariable("power_level", "?");

        var user = UserManager.getPlayer(player);

        if (user == null)
          return new InterpretationEnvironment().withVariable("power_level", "?");

        return new InterpretationEnvironment()
          .withVariable("power_level", user.getPowerLevel());
      }

      case PLAYER_NAME -> {
        return new InterpretationEnvironment()
          .withVariable("name", player.getName());
      }

      case TPS -> {
        return new InterpretationEnvironment()
          .withVariable("tps", Bukkit.getServer().getTPS());
      }
    }

    return new InterpretationEnvironment();
  }

  private static boolean isColorChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9') || (c >= 'k' && c <= 'o') || c == 'r';
  }

  private static boolean isHexChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }

  private static String enableColors(String input) {
    var inputLength = input.length();
    var result = new StringBuilder(inputLength);

    for (var charIndex = 0; charIndex < inputLength; ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var remainingChars = inputLength - 1 - charIndex;

      if (currentChar != '&' || remainingChars == 0) {
        result.append(currentChar);
        continue;
      }

      var nextChar = input.charAt(++charIndex);

      // Possible hex-sequence of format &#RRGGBB
      if (nextChar == '#' && remainingChars >= 6 + 1) {
        var r1 = input.charAt(charIndex + 1);
        var r2 = input.charAt(charIndex + 2);
        var g1 = input.charAt(charIndex + 3);
        var g2 = input.charAt(charIndex + 4);
        var b1 = input.charAt(charIndex + 5);
        var b2 = input.charAt(charIndex + 6);

        if (isHexChar(r1) && isHexChar(r2) && isHexChar(g1) && isHexChar(g2) && isHexChar(b1) && isHexChar(b2)) {
          result
            .append('§').append('x')
            .append('§').append(r1)
            .append('§').append(r2)
            .append('§').append(g1)
            .append('§').append(g2)
            .append('§').append(b1)
            .append('§').append(b2);

          charIndex += 6;
          continue;
        }
      }

      // Vanilla color-sequence
      if (isColorChar(nextChar)) {
        result.append('§').append(nextChar);
        continue;
      }

      // Wasn't a color-sequence, store as-is
      result.append(currentChar).append(nextChar);
    }

    return result.toString();
  }
}

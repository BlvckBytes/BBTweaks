package me.blvckbytes.bbtweaks.sidebar;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.sidebar.preferences.SidebarPreferences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.Objects;

public class SidebarBoard {

  private static final int MAX_SCORE_COUNT = 15;

  private static final String[] FORMATTING_SEQUENCES = {
    "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9",
    "§a", "§b", "§c", "§d", "§e", "§f",
    "§k", "§l", "§m", "§n", "§o", "§r"
  };

  private final String pluginName;

  public final BoardHolder holder;

  private final ConfigKeeper<MainSection> config;

  private final Int2ObjectMap<ScoreAndTeam> cachedScoreAndTeamByIndex;

  private int currentScrollOffset;
  private long lastScrollChangeTime;

  public SidebarBoard(Plugin plugin, BoardHolder holder, ConfigKeeper<MainSection> config) {
    this.pluginName = plugin.getName().toLowerCase();

    this.holder = holder;
    this.config = config;
    this.cachedScoreAndTeamByIndex = new Int2ObjectOpenHashMap<>();
  }

  private List<? extends Component> paginate(long relativeTime, List<? extends Component> lines) {
    var excessLineCount = lines.size() - MAX_SCORE_COUNT;

    if (excessLineCount <= 0) {
      currentScrollOffset = 0;
      lastScrollChangeTime = 0;
      return lines;
    }

    if (lastScrollChangeTime == 0)
      lastScrollChangeTime = relativeTime;

    else if (relativeTime - lastScrollChangeTime >= config.rootSection.sidebar.scrollIntervalTicks) {
      lastScrollChangeTime = relativeTime;

      if (++currentScrollOffset > excessLineCount)
        currentScrollOffset = 0;
    }

    return lines.subList(currentScrollOffset, currentScrollOffset + MAX_SCORE_COUNT);
  }

  public void advanceScrollingAndSetLines(long relativeTime, List<? extends Component> lines, SidebarPreferences preferences) {
    var scoreboard = holder.bukkitPlayer().getScoreboard();
    var sidebarObjective = scoreboard.getObjective(DisplaySlot.SIDEBAR);

    if (sidebarObjective == null) {
      sidebarObjective = scoreboard.registerNewObjective(pluginName, Criteria.DUMMY, preferences.getBoardTitle());
      sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
      sidebarObjective.numberFormat(NumberFormat.blank());
      cachedScoreAndTeamByIndex.clear();
    }

    // Do not modify foreign objectives - simply NOOP in that case.
    if (!sidebarObjective.getName().equals(pluginName))
      return;

    var boardTitle = preferences.getBoardTitle();

    if (!sidebarObjective.displayName().equals(boardTitle))
      sidebarObjective.displayName(boardTitle);

    if (lines.isEmpty()) {
      cachedScoreAndTeamByIndex.values().forEach(it -> it.score().resetScore());
      return;
    }

    lines = paginate(relativeTime, lines);

    // Note: Calling into set-/reset-/prefix-methods marks the board/team as dirty and creates
    //       update packets; let's diff-check locally beforehand, as it's cheap enough.

    var maxSize = Math.max(lines.size(), cachedScoreAndTeamByIndex.size());

    for (var index = 0; index < maxSize; ++index) {
      var scoreAndTeam = accessScoreAndTeam(scoreboard, sidebarObjective, index);

      if (index >= lines.size()) {
        if (scoreAndTeam.score().isScoreSet())
          scoreAndTeam.score().resetScore();

        continue;
      }

      if (scoreAndTeam.score().getScore() != maxSize - index)
        scoreAndTeam.score().setScore(maxSize - index);

      var line = lines.get(index);

      if (!Objects.equals(line, scoreAndTeam.team().prefix()))
        scoreAndTeam.team().prefix(line);
    }
  }

  public void unregisterIfShown() {
    cachedScoreAndTeamByIndex.clear();

    var scoreboard = holder.bukkitPlayer().getScoreboard();
    var sidebarObjective = scoreboard.getObjective(DisplaySlot.SIDEBAR);

    if (sidebarObjective != null && sidebarObjective.getName().equals(pluginName)) {
      for (var team : scoreboard.getTeams()) {
        if (team.getName().startsWith(pluginName + ":t"))
          team.unregister();
      }

      sidebarObjective.unregister();
    }
  }

  private ScoreAndTeam accessScoreAndTeam(Scoreboard scoreboard, Objective sidebarObjective, int index) {
    return cachedScoreAndTeamByIndex.computeIfAbsent(index, k -> {
      var uniqueHolderName = makeUniqueScoreHolderName(index);
      var teamName = pluginName + ":t" + index;

      var team = scoreboard.getTeam(teamName);

      if (team == null)
        team = scoreboard.registerNewTeam(teamName);

      var score = sidebarObjective.getScore(uniqueHolderName);

      if (!team.hasEntry(uniqueHolderName))
        team.addEntry(uniqueHolderName);

      return new ScoreAndTeam(score, team);
    });
  }

  private String makeUniqueScoreHolderName(int index) {
    if (index < 0)
      throw new IllegalStateException("Index must be greater than or equal to zero");

    // NOTE: As I've just learned the hard way, Minecraft does not (yet) support more than
    //       15 entries; I'm keeping this more versatile scheme, if they ever allow more.

    if (index < FORMATTING_SEQUENCES.length)
      return FORMATTING_SEQUENCES[index];

    var result = new StringBuilder(4);

    while (index >= 0) {
      result.append(FORMATTING_SEQUENCES[Math.min(index, FORMATTING_SEQUENCES.length - 1)]);
      index -= FORMATTING_SEQUENCES.length;
    }

    return result.toString();
  }
}

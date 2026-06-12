package me.blvckbytes.bbtweaks.ab_sleep;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;

public class ActionBarSleepMessage implements Tickable {

  private static final int DISPLAY_UPDATE_PERIOD_T = 5;

  private final ConfigKeeper<MainSection> config;

  public ActionBarSleepMessage(ConfigKeeper<MainSection> config) {
    this.config = config;
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % DISPLAY_UPDATE_PERIOD_T != 0)
      return;

    for (var world : Bukkit.getWorlds()) {
      var worldMembers = world.getPlayers();
      var sleepingPercentage = world.getGameRuleValue(GameRules.PLAYERS_SLEEPING_PERCENTAGE);

      var isAnySleeping = false;
      var nonIgnoredSleepCandidateCount = 0;
      var nonIgnoredSleepingCount = 0;

      for (var worldMember : worldMembers) {
        var isMemberSleeping = worldMember.isSleeping();

        isAnySleeping |= isMemberSleeping;

        if (worldMember.isSleepingIgnored())
          continue;

        ++nonIgnoredSleepCandidateCount;

        if (isMemberSleeping)
          ++nonIgnoredSleepingCount;
      }

      if (!isAnySleeping)
        continue;

      var thresholdCount = (int) Math.ceil(nonIgnoredSleepCandidateCount * (sleepingPercentage / 100.0));
      var reachedThreshold = nonIgnoredSleepingCount >= thresholdCount;

      var message = reachedThreshold ? config.rootSection.abSleep.thresholdReached : config.rootSection.abSleep.thresholdNotYetReached;

      var environment = new InterpretationEnvironment()
        .withVariable("sleeping_count", nonIgnoredSleepingCount)
        .withVariable("candidate_count", nonIgnoredSleepCandidateCount)
        .withVariable("threshold_count", thresholdCount);

      var component = message.interpret(SlotType.SINGLE_LINE_CHAT, environment).get(0);

      for (var worldMember : worldMembers)
        worldMember.sendActionBar(component);
    }
  }
}

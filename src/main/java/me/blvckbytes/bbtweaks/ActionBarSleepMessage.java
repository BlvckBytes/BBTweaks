package me.blvckbytes.bbtweaks;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;

public class ActionBarSleepMessage {

  public ActionBarSleepMessage(BBTweaksPlugin plugin) {
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      for (var world : Bukkit.getWorlds()) {
        var worldMembers = world.getPlayers();
        var sleepingPercentage = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE);

        if (sleepingPercentage == null)
          sleepingPercentage = 50;

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
        var configuredMessage = plugin.accessConfigValue(
          "actionBar." + (reachedThreshold ? "thresholdReached" : "thresholdNotYetReached")
        );

        //noinspection deprecation
        var parameterizedMessage = TextComponent.fromLegacyText(
          configuredMessage
            .replace("{sleeping_count}", String.valueOf(nonIgnoredSleepingCount))
            .replace("{candidate_count}", String.valueOf(nonIgnoredSleepCandidateCount))
            .replace("{threshold_count}", String.valueOf(thresholdCount))
        );

        for (var worldMember : worldMembers)
          worldMember.spigot().sendMessage(ChatMessageType.ACTION_BAR, parameterizedMessage);
      }
    }, 0, 5);
  }
}

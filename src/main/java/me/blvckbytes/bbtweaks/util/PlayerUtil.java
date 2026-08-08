package me.blvckbytes.bbtweaks.util;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PlayerUtil {

  public static List<String> suggestPlayerNames(String input, @Nullable Predicate<Player> predicate) {
    var result = new ArrayList<String>();

    for (var player : Bukkit.getOnlinePlayers()) {
      if (predicate != null && !predicate.test(player))
        continue;

      if (StringUtils.startsWithIgnoreCase(player.getName(), input))
        result.add(player.getName());

      var displayName = ComponentUtil.asTrimmedText(player.displayName());

      if (StringUtils.startsWithIgnoreCase(displayName, input))
        result.add(displayName);
    }

    return result;
  }

  public static @Nullable Player getPlayerByName(String name) {
    for (var player : Bukkit.getOnlinePlayers()) {
      if (player.getName().equalsIgnoreCase(name))
        return player;

      var displayName = ComponentUtil.asTrimmedText(player.displayName());

      if (displayName.equalsIgnoreCase(name))
        return player;
    }

    return null;
  }
}

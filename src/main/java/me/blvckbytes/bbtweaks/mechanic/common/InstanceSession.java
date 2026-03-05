package me.blvckbytes.bbtweaks.mechanic.common;

import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;
import org.bukkit.entity.Player;

import java.util.Map;

public record InstanceSession<InstanceType extends MechanicInstance>(
  Player player,
  InstanceType instance,
  int creationTime
) {
  public static <InstanceType extends MechanicInstance> void handleSessionTimeouts(
    Map<?, InstanceSession<InstanceType>> sessionMap,
    int time, int timeoutSeconds,
    TimeoutHandler<InstanceType> timeoutHandler
  ) {
    for (var iterator = sessionMap.values().iterator(); iterator.hasNext();) {
      var session = iterator.next();

      if (time - session.creationTime < timeoutSeconds * 20)
        continue;

      iterator.remove();
      timeoutHandler.handle(session, timeoutSeconds);
    }
  }
}

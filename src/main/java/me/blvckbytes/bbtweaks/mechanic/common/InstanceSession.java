package me.blvckbytes.bbtweaks.mechanic.common;

import me.blvckbytes.bbtweaks.mechanic.MechanicInstance;
import org.bukkit.entity.Player;

import java.util.Map;

public record InstanceSession<InstanceType extends MechanicInstance>(
  Player player,
  InstanceType instance,
  long creationTime
) {
  public static <InstanceType extends MechanicInstance> void handleSessionTimeouts(
    Map<?, InstanceSession<InstanceType>> sessionMap,
    long time, long timeoutSeconds,
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

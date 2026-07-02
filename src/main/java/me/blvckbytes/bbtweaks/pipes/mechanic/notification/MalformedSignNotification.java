package me.blvckbytes.bbtweaks.pipes.mechanic.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MalformedSignNotification extends PipeNotification {

  private final Location signLocation;
  private final String malformedToken;
  private final int lineNumber;

  public MalformedSignNotification(Location signLocation, String malformedToken, int lineNumber) {
    super(NotificationFlag.BROADCAST_TO_REGION);

    this.signLocation = signLocation;
    this.malformedToken = malformedToken;
    this.lineNumber = lineNumber;
  }

  @Override
  public ComponentMarkup getMessage(ConfigKeeper<MainSection> config) {
    return config.rootSection.pipes.notifications.malformedSignToken;
  }

  @Override
  public InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates) {
    return new InterpretationEnvironment()
      .withVariable("coordinates", extendedCoordinates)
      .withVariable("sign_coordinates", signLocation.getBlockX() + " " + signLocation.getBlockY() + " " + signLocation.getBlockZ())
      .withVariable("line", lineNumber)
      .withVariable("token", malformedToken);
  }

  @Override
  public @Nullable Object[] getDataTokens() {
    // Let's just debounce on the sign itself. The player will be notified a bit later
    // anyway, if more invalid tokens remain. This approach reduces needless spam.
    return new Object[] { signLocation.getBlockX(), signLocation.getBlockY(), signLocation.getBlockZ() };
  }
}

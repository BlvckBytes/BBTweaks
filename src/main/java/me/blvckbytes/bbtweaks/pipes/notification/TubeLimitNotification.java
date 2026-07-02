package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TubeLimitNotification extends PipeNotification {

  private final int tubeLimit;

  public TubeLimitNotification(int tubeLimit) {
    super(NotificationFlag.BROADCAST_TO_REGION);

    this.tubeLimit = tubeLimit;
  }

  @Override
  public ComponentMarkup getMessage(ConfigKeeper<MainSection> config) {
    return config.rootSection.pipes.notifications.tubeLimitExceeded;
  }

  @Override
  public InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates) {
    return new InterpretationEnvironment()
      .withVariable("coordinates", extendedCoordinates)
      .withVariable("limit", tubeLimit);
  }

  @Override
  public @Nullable Object[] getDataTokens() {
    return new Object[0];
  }
}

package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PistonLimitNotification extends PipeNotification {

  private final int pistonLimit;

  public PistonLimitNotification(int pistonLimit) {
    super(NotificationFlag.BROADCAST_TO_REGION);

    this.pistonLimit = pistonLimit;
  }

  @Override
  public ComponentMarkup getMessage(ConfigKeeper<MainSection> config) {
    return config.rootSection.pipes.notifications.pistonLimitExceeded;
  }

  @Override
  public InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates) {
    return new InterpretationEnvironment()
      .withVariable("coordinates", extendedCoordinates)
      .withVariable("limit", pistonLimit);
  }

  @Override
  public @Nullable Object[] getDataTokens() {
    return new Object[0];
  }
}

package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class NoSignNotification extends PipeNotification {

  public NoSignNotification() {
    super(NotificationFlag.BROADCAST_TO_REGION);
  }

  @Override
  public ComponentMarkup getMessage(ConfigKeeper<MainSection> config) {
    return config.rootSection.pipes.notifications.noSignEncountered;
  }

  @Override
  public InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates) {
    return new InterpretationEnvironment()
      .withVariable("coordinates", extendedCoordinates);
  }

  @Override
  public @Nullable Object[] getDataTokens() {
    return new Object[0];
  }
}

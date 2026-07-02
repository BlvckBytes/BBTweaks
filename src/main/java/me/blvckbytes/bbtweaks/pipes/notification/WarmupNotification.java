package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class WarmupNotification extends PipeNotification {

  private final int pistonBlockCount;
  private final int tubeBlockCount;

  public WarmupNotification(int pistonBlockCount, int tubeBlockCount) {
    super(NotificationFlag.SEND_IN_ACTION_BAR);

    this.pistonBlockCount = pistonBlockCount;
    this.tubeBlockCount = tubeBlockCount;
  }

  @Override
  public ComponentMarkup getMessage(ConfigKeeper<MainSection> config) {
    return config.rootSection.pipes.notifications.warmingUp;
  }

  @Override
  public InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates) {
    return new InterpretationEnvironment()
      .withVariable("coordinates", extendedCoordinates)
      .withVariable("tubes", tubeBlockCount)
      .withVariable("pistons", pistonBlockCount);
  }

  @Override
  public @Nullable String[] getDataTokens() {
    // This notification is supposed to be "spammy", as to keep the action-bar-message visible with full opacity
    return null;
  }
}

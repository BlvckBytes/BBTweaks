package me.blvckbytes.bbtweaks.pipes.notification;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import me.blvckbytes.bbtweaks.MainSection;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class PipeNotification {

  public final Set<NotificationFlag> flags;

  private final Set<UUID> receiverIds = new HashSet<>();

  public PipeNotification(NotificationFlag... flags) {
    this.flags = Collections.unmodifiableSet(flags.length == 0 ? EnumSet.noneOf(NotificationFlag.class) : EnumSet.of(flags[0], flags));
  }

  public abstract ComponentMarkup getMessage(ConfigKeeper<MainSection> config);

  public abstract InterpretationEnvironment buildMessageEnvironment(Player receiver, String extendedCoordinates);

  /**
   * @return The tokens which identify the information conveyed to the user; this is used
   *         to debounce notifications which would needlessly spam the chat. Return null
   *         to entirely deactivate debouncing this notification; return an empty array if
   *         there's no additional information, but the type of notification should still
   *         be debounced.
   */
  public abstract @Nullable Object[] getDataTokens();

  public @Nullable String makeDebounceId(Block inputPistonBlock) {
    var dataTokens = getDataTokens();

    if (dataTokens == null)
      return null;

    var result = new StringJoiner("_");

    var inputPistonTokens = new Object[] {
      inputPistonBlock.getWorld().getName(),
      inputPistonBlock.getX(), inputPistonBlock.getY(), inputPistonBlock.getZ()
    };

    for (var inputPistonToken : inputPistonTokens)
      result.add(String.valueOf(inputPistonToken));

    result.add(getClass().getSimpleName());

    for (var dataToken : getDataTokens())
      result.add(String.valueOf(dataToken));

    return result.toString();
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean addReceiver(Player receiver) {
    return receiverIds.add(receiver.getUniqueId());
  }
}

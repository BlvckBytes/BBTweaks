package me.blvckbytes.bbtweaks.back;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BacktrackSession {

  private final Plugin plugin;
  public final Player player;
  private final String commandLabel;
  public final Location initialLocation;
  private final LocationHistory history;

  private final List<Location> locations;
  private int currentLocationIndex;

  private BacktrackSession(Plugin plugin, Player player, String commandLabel, LocationHistory history) {
    this.plugin = plugin;
    this.player = player;
    this.commandLabel = commandLabel;
    this.initialLocation = player.getLocation();
    this.history = history;

    var length = history.maxSize();
    this.locations = new ArrayList<>(length + 1);

    locations.add(initialLocation);

    for (var index = 0; index < history.maxSize(); ++index) {
      var location = history.getNthLastLocation(index);

      if (location != null)
        locations.add(location);
    }

    if (!locations.isEmpty())
      teleportNoBack(locations.get(0));
  }

  public static BacktrackSession captureHistoryAndStartAtOrigin(Plugin plugin, Player player, String commandLabel, LocationHistory history) {
    return new BacktrackSession(plugin, player, commandLabel, history);
  }

  public void onEnd(boolean resetHud, boolean addToHistory) {
    if (resetHud) {
      player.resetTitle();
      player.sendActionBar(Component.empty());
    }

    // Only push the initial location into the history if the player exited somewhere other
    // than the first entry of the locations-list, seeing how that is defined to be the
    // initial location; only then do they have a reason to use /back to return.
    if (addToHistory && currentLocationIndex != 0)
      history.add(initialLocation);
  }

  public InterpretationEnvironment makeEnvironment() {
    return new InterpretationEnvironment()
      .withVariable("label", commandLabel)
      // Zero is defined to be the origin
      .withVariable("current_position", currentLocationIndex)
      // Minus the origin
      .withVariable("total_count", locations.size() - 1);
  }

  public void previous() {
    if (currentLocationIndex == 0)
      currentLocationIndex = locations.size();

    teleportNoBack(locations.get(--currentLocationIndex));
  }

  public void next() {
    if (currentLocationIndex >= locations.size() - 1)
      currentLocationIndex = -1;

    teleportNoBack(locations.get(++currentLocationIndex));
  }

  @SuppressWarnings("deprecation")
  private void teleportNoBack(Location location) {
    player.setMetadata("essentials:ignore-teleport", new FixedMetadataValue(plugin, true));
    player.teleport(location);
    player.removeMetadata("essentials:ignore-teleport", plugin);
  }
}

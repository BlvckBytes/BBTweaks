package me.blvckbytes.bbtweaks.back;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LocationHistoryAddEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private final Player player;
  private final LocationHistory locationHistory;
  private @NotNull Location location;

  private boolean cancelled;

  public LocationHistoryAddEvent(
    Player player,
    LocationHistory locationHistory,
    @NotNull Location location
  ) {
    this.player = player;
    this.locationHistory = locationHistory;
    this.location = location;
  }

  public Player getPlayer() {
    return player;
  }

  public LocationHistory getLocationHistory() {
    return locationHistory;
  }

  public @NotNull Location getLocation() {
    return location;
  }

  public void setLocation(@NotNull Location location) {
    this.location = location;
  }

  @Override
  @NotNull
  public HandlerList getHandlers() {
    return handlers;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}

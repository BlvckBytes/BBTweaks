package me.blvckbytes.bbtweaks.back;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class LocationHistory {

  private static final int HISTORY_SIZE = 5;

  private final Location[] historyRingbuffer;
  private int nextWriteIndex;

  public LocationHistory() {
    this.historyRingbuffer = new Location[HISTORY_SIZE];
  }

  public void add(Location location) {
    historyRingbuffer[nextWriteIndex++] = location;

    if (nextWriteIndex >= historyRingbuffer.length)
      nextWriteIndex = 0;
  }

  public @Nullable Location getLastLocation() {
    var readIndex = nextWriteIndex - 1;

    if (readIndex < 0)
      readIndex += historyRingbuffer.length;

    return historyRingbuffer[readIndex];
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("nextWriteIndex", nextWriteIndex);

    var history = new JsonArray();
    result.add("history", history);

    for (var location : historyRingbuffer) {
      JsonObject locationJson;

      if (location == null || (locationJson = locationToJson(location)) == null) {
        history.add(JsonNull.INSTANCE);
        continue;
      }

      history.add(locationJson);
    }

    return result;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder("[");

    for (var index = 0; index < historyRingbuffer.length; ++index) {
      if (index != 0)
        builder.append(", ");

      if (index == nextWriteIndex)
        builder.append("N ");

      var location = historyRingbuffer[index];

      if (location == null) {
        builder.append("null");
        continue;
      }

      builder
        .append(location.getBlockX())
        .append(' ')
        .append(location.getBlockY())
        .append(' ')
        .append(location.getBlockZ());
    }

    return builder.append(']').toString();
  }

  public static @Nullable LocationHistory fromJson(JsonObject json) {
    if (!(json.get("history") instanceof JsonArray history))
      return null;

    var result = new LocationHistory();

    for (var historyIndex = 0; historyIndex < history.size(); ++historyIndex) {
      if (historyIndex >= result.historyRingbuffer.length)
        break;

      if (history.get(historyIndex) instanceof JsonObject locationObject)
        result.historyRingbuffer[historyIndex] = locationFromJson(locationObject);
    }

    if (!(json.get("nextWriteIndex") instanceof JsonPrimitive indexPrimitive))
      return null;

    try {
      result.nextWriteIndex = indexPrimitive.getAsInt();

      if (result.nextWriteIndex >= result.historyRingbuffer.length)
        result.nextWriteIndex = 0;
    } catch (Throwable ignored) {}

    return result;
  }

  private static @Nullable JsonObject locationToJson(Location location) {
    var result = new JsonObject();

    var world = location.getWorld();

    if (world == null)
      return null;

    result.addProperty("world", world.getName());
    result.addProperty("x", location.getX());
    result.addProperty("y", location.getY());
    result.addProperty("z", location.getZ());
    result.addProperty("yaw", location.getYaw());
    result.addProperty("pitch", location.getPitch());

    return result;
  }

  private static @Nullable Location locationFromJson(JsonObject json) {
    if (!(json.get("world") instanceof JsonPrimitive world))
      return null;

    var bukkitWorld = Bukkit.getWorld(world.getAsString());

    if (bukkitWorld == null)
      return null;

    try {
      return new Location(
        bukkitWorld,
        json.get("x").getAsDouble(),
        json.get("y").getAsDouble(),
        json.get("z").getAsDouble(),
        json.get("yaw").getAsFloat(),
        json.get("pitch").getAsFloat()
      );
    } catch (Throwable e) {
      return null;
    }
  }
}

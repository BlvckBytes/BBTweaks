package me.blvckbytes.bbtweaks.back;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class LocationHistory {

  private static final int HISTORY_SIZE = 5;

  private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##", DECIMAL_SYMBOLS);

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

  public int maxSize() {
    return historyRingbuffer.length;
  }

  public @Nullable Location getNthLastLocation(int index) {
    var readIndex = nextWriteIndex - 1 - index;

    while (readIndex < 0)
      readIndex += historyRingbuffer.length;

    return historyRingbuffer[readIndex];
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    result.addProperty("nextWriteIndex", nextWriteIndex);

    var history = new JsonArray();
    result.add("history", history);

    for (var location : historyRingbuffer) {
      JsonPrimitive locationJson;

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

      if (history.get(historyIndex) instanceof JsonPrimitive locationPrimitive)
        result.historyRingbuffer[historyIndex] = locationFromJson(locationPrimitive);
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

  private static @Nullable JsonPrimitive locationToJson(Location location) {
    var world = location.getWorld();

    if (world == null)
      return null;

    return new JsonPrimitive(
      DECIMAL_FORMAT.format(location.getX()) + ' '
      + DECIMAL_FORMAT.format(location.getY()) + ' '
      + DECIMAL_FORMAT.format(location.getZ()) + ' '
      + DECIMAL_FORMAT.format(location.getYaw()) + ' '
      + DECIMAL_FORMAT.format(location.getPitch()) + ' '
      + world.getName()
    );
  }

  private static @Nullable Location locationFromJson(JsonPrimitive json) {
    var parts = json.getAsString().split(" ");

    if (parts.length != 6)
      return null;

    var bukkitWorld = Bukkit.getWorld(parts[5]);

    if (bukkitWorld == null)
      return null;

    try {
      return new Location(
        bukkitWorld,
        Double.parseDouble(parts[0]),
        Double.parseDouble(parts[1]),
        Double.parseDouble(parts[2]),
        Float.parseFloat(parts[3]),
        Float.parseFloat(parts[4])
      );
    } catch (Throwable e) {
      return null;
    }
  }
}

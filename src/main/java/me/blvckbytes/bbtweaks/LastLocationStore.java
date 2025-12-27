package me.blvckbytes.bbtweaks;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class LastLocationStore {

  private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();

  private final Plugin plugin;
  private final File storageFile;
  private final Map<UUID, Location> lastLocationByPlayerId;

  public LastLocationStore(Plugin plugin) {
    this.plugin = plugin;
    this.storageFile = new File(plugin.getDataFolder(), "last-location-store.json");

    if (!storageFile.exists()) {
      var parent = storageFile.getParentFile();

      if (!parent.isDirectory()) {
        if (!parent.mkdirs())
          throw new IllegalStateException("Could not create directory " + parent);
      }

      try {
        if (!storageFile.createNewFile())
          throw new IllegalStateException("Operation not successful");
      } catch (Throwable e) {
        throw new IllegalStateException("Could not create file: " + storageFile, e);
      }
    }

    this.lastLocationByPlayerId = new HashMap<>();
    this.loadAsync();
  }

  public void setLastLocation(Player player, Location location) {
    lastLocationByPlayerId.put(player.getUniqueId(), location);
  }

  public @Nullable Location getLastLocation(Player player) {
    return lastLocationByPlayerId.get(player.getUniqueId());
  }

  private void loadAsync() {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      if (storageFile.length() == 0)
        return;

      this.lastLocationByPlayerId.clear();

      try (var fileReader = new FileReader(storageFile)) {
        var locationMap = GSON_INSTANCE.fromJson(fileReader, JsonObject.class);

        for (var entry : locationMap.entrySet()) {
          UUID playerId;

          try {
            playerId = UUID.fromString(entry.getKey());
          } catch (IllegalArgumentException e) {
            continue;
          }

          if (!(entry.getValue() instanceof JsonObject object))
            continue;

          var location = locationFromJson(object);

          if (location == null)
            continue;

          lastLocationByPlayerId.put(playerId, location);
        }

        plugin.getLogger().info("Loaded " + lastLocationByPlayerId.size() + " last locations!");
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "Could not load " + storageFile, e);
      }
    });
  }

  public void save() {
    var locationMap = new JsonObject();

    for (var entry : lastLocationByPlayerId.entrySet()) {
      var jsonLocation = locationToJson(entry.getValue());

      if (jsonLocation != null)
        locationMap.add(entry.getKey().toString(), jsonLocation);
    }

    try (var writer = new FileWriter(storageFile)) {
      GSON_INSTANCE.toJson(locationMap, writer);
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not store " + storageFile, e);
    }
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

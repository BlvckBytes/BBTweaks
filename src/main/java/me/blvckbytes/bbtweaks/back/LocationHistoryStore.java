package me.blvckbytes.bbtweaks.back;

import com.google.gson.*;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.logging.Level;

public class LocationHistoryStore implements Tickable, Disableable {

  private static final Gson GSON_INSTANCE = new GsonBuilder().setPrettyPrinting().create();

  private static final long SAVE_PERIOD_T = 20 * 60L;

  private final Plugin plugin;
  private final File storageFile;
  private final Map<UUID, LocationHistory> historyByPlayerId;

  public LocationHistoryStore(Plugin plugin) {
    this.plugin = plugin;
    this.storageFile = new File(plugin.getDataFolder(), "location-history-store.json");

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

    this.historyByPlayerId = new HashMap<>();
    this.loadAsync();
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % SAVE_PERIOD_T == 0)
      save(true);
  }

  @Override
  public void disable() {
    save(false);
  }

  public LocationHistory accessHistory(Player player) {
    var playerId = player.getUniqueId();
    return historyByPlayerId.computeIfAbsent(playerId, _ -> new LocationHistory(playerId));
  }

  private void loadAsync() {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      if (storageFile.length() == 0)
        return;

      this.historyByPlayerId.clear();

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

          var history = LocationHistory.fromJson(playerId, object);

          if (history == null) {
            history = new LocationHistory(playerId);
            plugin.getLogger().log(Level.WARNING, "Could not load location-history for " + playerId + "; starting over from a blank slate");
          }

          historyByPlayerId.put(playerId, history);
        }

        plugin.getLogger().info("Loaded " + historyByPlayerId.size() + " location-histories!");
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "Could not load " + storageFile, e);
      }
    });
  }

  private void save(boolean async) {
    var histories = new ArrayList<>(historyByPlayerId.values());

    if (!async) {
      saveHistories(histories);
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveHistories(histories));
  }

  private void saveHistories(List<LocationHistory> histories) {
    var locationMap = new JsonObject();

    for (var history : histories)
      locationMap.add(history.playerId.toString(), history.toJson());

    try (var writer = new FileWriter(storageFile)) {
      GSON_INSTANCE.toJson(locationMap, writer);
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Could not store " + storageFile, e);
    }
  }
}

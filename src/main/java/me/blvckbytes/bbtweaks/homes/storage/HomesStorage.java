package me.blvckbytes.bbtweaks.homes.storage;

import com.google.gson.*;
import me.blvckbytes.bbtweaks.auto_wirer.Disableable;
import me.blvckbytes.bbtweaks.auto_wirer.Tickable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HomesStorage implements Disableable, Tickable {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final long SAVE_PERIOD_T = 20 * 60 * 5;

  private final Plugin plugin;
  private final File homesDirectory;

  private final Map<UUID, PlayerHomes> playerHomesByPlayerId;
  private final Map<String, KnownPlayer> knownPlayerByLastKnownNameLower;

  public HomesStorage(Plugin plugin) {
    this.plugin = plugin;
    this.homesDirectory = new File(plugin.getDataFolder(), "homes");

    if (!homesDirectory.exists()) {
      if (!homesDirectory.mkdirs())
        throw new IllegalStateException("Could not create " + homesDirectory);
    } else if (!homesDirectory.isDirectory())
      throw new IllegalStateException("Expected directory at " + homesDirectory);

    this.playerHomesByPlayerId = new HashMap<>();
    this.knownPlayerByLastKnownNameLower = new HashMap<>();

    loadAllHomes();
  }

  @Override
  public void disable() {
    saveDirtyHomes(true);
  }

  @Override
  public void tick(long relativeTime) {
    if (relativeTime % SAVE_PERIOD_T == 0)
      saveDirtyHomes(false);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var knownPlayer = new KnownPlayer(event.getPlayer());

    accessHomes(knownPlayer).setLastKnownName(knownPlayer.lastKnownName());
    knownPlayerByLastKnownNameLower.put(knownPlayer.lastKnownName().toLowerCase(), knownPlayer);
  }

  public PlayerHomes accessHomes(Player player) {
    return playerHomesByPlayerId.computeIfAbsent(player.getUniqueId(), _ -> {
      var playerHomes = new PlayerHomes(player.getName());

      // Ensure that even if the player does not set any homes, they are remembered as a known player.
      playerHomes.markDirty();

      return playerHomes;
    });
  }

  public PlayerHomes accessHomes(KnownPlayer player) {
    return playerHomesByPlayerId.computeIfAbsent(player.playerId(), _ -> new PlayerHomes(player.lastKnownName()));
  }

  public @Nullable KnownPlayer getKnownPlayerByName(String name) {
    return knownPlayerByLastKnownNameLower.get(name.toLowerCase());
  }

  private void saveDirtyHomes(boolean sync) {
    var jsonByPlayerId = new HashMap<UUID, JsonObject>();

    for (var entry : playerHomesByPlayerId.entrySet()) {
      var playerHomes = entry.getValue();

      if (!playerHomes.isDirty())
        return;

      try {
        jsonByPlayerId.put(entry.getKey(), playerHomes.toJson());
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "An error occurred while trying to jsonify homes for " + entry.getKey(), e);
      }
    }

    if (sync) {
      writeHomesToFiles(jsonByPlayerId);
      return;
    }

    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeHomesToFiles(jsonByPlayerId));
  }

  private void writeHomesToFiles(Map<UUID, JsonObject> homesJsonByPlayerId) {
    for (var entry : homesJsonByPlayerId.entrySet()) {
      var playerId = entry.getKey();

      try {
        writeHomesToFile(playerId, entry.getValue());
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "An error occurred while trying to save home-data for " + entry.getKey(), e);
        continue;
      }

      var playerHomes = playerHomesByPlayerId.get(playerId);

      if (playerHomes != null)
        playerHomes.clearDirty();
    }
  }

  private void writeHomesToFile(UUID playerId, JsonObject homesJson) throws Exception {
    var file = new File(homesDirectory, playerId + ".json");

    try (var writer = new FileWriter(file)) {
      GSON.toJson(homesJson, writer);
    }
  }

  private void loadAllHomes() {
    var homeFiles = homesDirectory.listFiles();

    if (homeFiles == null)
      return;

    var totalHomeCounter = 0;
    var loadedFileCounter = 0;

    for (var homeFile : homeFiles) {
      var fileName = homeFile.getName();

      if (!homeFile.isFile() || !fileName.endsWith(".json") || homeFile.length() == 0)
        continue;

      var playerIdString = fileName.substring(0, fileName.indexOf('.'));
      UUID playerId;

      try {
        playerId = UUID.fromString(playerIdString);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "Encountered malformed uuid on file-name of home-file " + homeFile, e);
        continue;
      }

      JsonElement json;

      try (var fileReader = new FileReader(homeFile)) {
        json = GSON.fromJson(fileReader, JsonElement.class);
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "Could not read json from malformed home-file " + homeFile, e);
        continue;
      }

      if (!(json instanceof JsonObject homeFileObject)) {
        plugin.getLogger().warning("Expected top-level object in home-file " + homeFile);
        continue;
      }

      PlayerHomes playerHomes;

      try {
        playerHomes = PlayerHomes.fromJson(homeFileObject, homeFile, plugin.getLogger());
      } catch (Throwable e) {
        plugin.getLogger().log(Level.WARNING, "An error occurred while trying to load homes from " + homeFile, e);
        continue;
      }

      var lastKnownName = playerHomes.getLastKnownName();
      knownPlayerByLastKnownNameLower.put(lastKnownName.toLowerCase(), new KnownPlayer(lastKnownName, playerId));

      playerHomesByPlayerId.put(playerId, playerHomes);

      totalHomeCounter += playerHomes.getHomeCount();

      ++loadedFileCounter;
    }

    plugin.getLogger().info("Loaded " + totalHomeCounter + " total homes from " + loadedFileCounter + " files");
  }
}

package me.blvckbytes.bbtweaks.offline_inventory;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import me.blvckbytes.bbtweaks.util.ComponentUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

public class OfflinePlayerRegistry implements Listener {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

  private final Plugin plugin;

  private final List<String> knownNames;
  private final Map<String, UUID> idByNameLower;

  public OfflinePlayerRegistry(
    Plugin plugin
  ) {
    this.plugin = plugin;

    this.knownNames = new ArrayList<>();
    this.idByNameLower = new HashMap<>();

    for (var offlinePlayer : Bukkit.getOfflinePlayers()) {
      var name = offlinePlayer.getName();

      if (name != null)
        addKnownName(name, offlinePlayer.getUniqueId());
    }
  }

  public Stream<String> streamKnownNames() {
    return knownNames.stream();
  }

  public @Nullable OfflinePlayer getPlayerByName(String name) {
    var playerId = idByNameLower.get(name.toLowerCase());

    if (playerId == null)
      return null;

    return Bukkit.getOfflinePlayer(playerId);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    addKnownName(player.getName(), player.getUniqueId());

    var nameBuilder = new StringBuilder();

    ComponentUtil.forEachTextOfComponent(player.displayName(), part -> {
      part = part.trim();

      if (!part.isBlank())
        nameBuilder.append(part);
    });

    if (!nameBuilder.isEmpty())
      addKnownName(nameBuilder.toString(), player.getUniqueId());
  }

  private void addKnownName(String name, UUID playerId) {
    var trimmedName = name.trim();

    if (trimmedName.isBlank())
      return;

    if (knownNames.stream().anyMatch(it -> it.equalsIgnoreCase(trimmedName)))
      return;

    knownNames.add(trimmedName);
    idByNameLower.put(trimmedName.toLowerCase(), playerId);
  }

  public void tryResolvePlayerId(String name, Consumer<@Nullable UUID> idConsumer) {
    var cachedPlayer = Bukkit.getOfflinePlayerIfCached(name);

    if (cachedPlayer != null) {
      idConsumer.accept(cachedPlayer.getUniqueId());
      return;
    }

    try {
      var url = "https://api.mojang.com/users/profiles/minecraft/" + name;

      var request = HttpRequest.newBuilder(URI.create(url)).build();
      var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        idConsumer.accept(null);
        return;
      }

      if (response.statusCode() != 200)
        throw new IllegalStateException("Non-200 status-code for profile-resolve: " + response.statusCode());

      var jsonElement = JsonParser.parseString(response.body());

      if (jsonElement == null || !jsonElement.isJsonObject())
        throw new IllegalStateException("Non-json response");

      var rootObject = jsonElement.getAsJsonObject();

      if (!(rootObject.get("id") instanceof JsonPrimitive idPrimitive))
        throw new IllegalStateException("Missing \"id\"-property in profile-response");

      var idString = idPrimitive.getAsString();

      UUID id;

      try {
        id = fromDashLessString(idString);
      } catch (Throwable e) {
        throw new IllegalStateException("Received invalid dash-less \"id\"-value: " + idString);
      }

      idConsumer.accept(id);
    } catch (Exception e) {
      plugin.getLogger().log(Level.WARNING, "An error occurred while trying to fetch the uuid of " + name, e);
      idConsumer.accept(null);
    }
  }

  private static UUID fromDashLessString(String input) {
    if (input == null || input.length() != 32)
      throw new IllegalArgumentException("Invalid dash-less UUID: " + input);

    var sb = new StringBuilder(input);
    sb.insert(8, '-');
    sb.insert(13, '-');
    sb.insert(18, '-');
    sb.insert(23, '-');

    return UUID.fromString(sb.toString());
  }
}

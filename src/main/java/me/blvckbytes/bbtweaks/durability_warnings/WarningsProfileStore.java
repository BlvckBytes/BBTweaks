package me.blvckbytes.bbtweaks.durability_warnings;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarningsProfileStore implements Listener {

  private final NamespacedKey playSoundKey;
  private final NamespacedKey enabledKey;

  private final Map<UUID, WarningsProfile> profileByPlayerId;

  public WarningsProfileStore(Plugin plugin) {
    this.playSoundKey = new NamespacedKey(plugin, "durability-warnings-play-sound");
    this.enabledKey = new NamespacedKey(plugin, "durability-warnings-enabled");

    this.profileByPlayerId = new HashMap<>();
  }

  public WarningsProfile accessProfile(Player player) {
    return profileByPlayerId.computeIfAbsent(player.getUniqueId(), k -> loadProfile(player));
  }

  private WarningsProfile loadProfile(Player player) {
    var profile = new WarningsProfile(player);

    var pdc = player.getPersistentDataContainer();

    Boolean value;

    if ((value = pdc.get(playSoundKey, PersistentDataType.BOOLEAN)) != null)
      profile.playSound = value;

    if ((value = pdc.get(enabledKey, PersistentDataType.BOOLEAN)) != null)
      profile.enabled = value;

    return profile;
  }

  private void saveProfile(WarningsProfile profile) {
    var pdc = profile.player.getPersistentDataContainer();

    pdc.set(playSoundKey, PersistentDataType.BOOLEAN, profile.playSound);
    pdc.set(enabledKey, PersistentDataType.BOOLEAN, profile.enabled);
  }

  public void onShutdown() {
    for (var profile : profileByPlayerId.values())
      saveProfile(profile);

    profileByPlayerId.clear();
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    loadProfile(event.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var profile = profileByPlayerId.remove(event.getPlayer().getUniqueId());

    if (profile != null)
      saveProfile(profile);
  }
}

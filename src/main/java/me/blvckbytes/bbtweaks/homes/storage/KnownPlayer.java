package me.blvckbytes.bbtweaks.homes.storage;

import org.bukkit.entity.Player;

import java.util.UUID;

public record KnownPlayer(String lastKnownName, UUID playerId) {

  public KnownPlayer(Player player) {
    this(player.getName(), player.getUniqueId());
  }
}

package me.blvckbytes.bbtweaks.list_chunk_tickets;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.Set;

public record ExistingChunkTicket(
  Chunk chunk,
  Location teleportLocation,
  Set<String> pluginNames,
  Set<String> regionIds
) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "x" -> teleportLocation.getBlockX();
      case "y" -> teleportLocation.getBlockY();
      case "z" -> teleportLocation.getBlockZ();
      case "plugins" -> pluginNames;
      case "regions" -> regionIds;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("x", "y", "z", "plugins", "regions");
  }
}

package me.blvckbytes.bbtweaks.list_chunk_tickets;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import me.blvckbytes.bbtweaks.util.TeleportUtil;
import org.bukkit.Chunk;

import java.util.Set;

public record ExistingChunkTicket(Chunk chunk, Set<String> pluginNames) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "x" -> chunk.getX() << 4;
      case "y" -> {
        var location = TeleportUtil.findSafeTeleportLocation(chunk.getWorld(), chunk.getX() << 4, chunk.getZ() << 4);
        yield location == null ? 100 : location.getY();
      }
      case "z" -> chunk.getZ() << 4;
      case "plugins" -> pluginNames;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("x", "y", "z", "plugins");
  }
}

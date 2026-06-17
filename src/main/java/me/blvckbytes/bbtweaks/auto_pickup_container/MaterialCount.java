package me.blvckbytes.bbtweaks.auto_pickup_container;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import org.bukkit.Material;

import java.util.Set;

public record MaterialCount(Material material, int count) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "type" -> material.translationKey();
      case "count" -> count;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("type", "count");
  }
}

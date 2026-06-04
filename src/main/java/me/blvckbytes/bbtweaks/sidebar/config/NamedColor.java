package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.Set;

public record NamedColor(
  String name,
  Material iconType,
  Component displayName,
  String hexColor
) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "icon_type" -> iconType;
      case "display_name" -> displayName;
      case "hex" -> hexColor;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("icon_type", "display_name", "hex");
  }
}


package me.blvckbytes.bbtweaks.sign_copier;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import net.kyori.adventure.text.Component;

import java.util.Set;

public record CopiedLine(
  Component renderedLine,
  String editCommand
) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "render" -> renderedLine;
      case "edit_command" -> editCommand;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("render", "edit_command");
  }
}

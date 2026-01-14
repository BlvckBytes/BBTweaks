package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public record ChoiceEntry(int number, List<Component> results) implements DirectFieldAccess {

  @Override
  public @Nullable Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "number" -> number;
      case "results" -> results;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("number", "results");
  }
}

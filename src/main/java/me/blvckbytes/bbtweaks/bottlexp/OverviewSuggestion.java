package me.blvckbytes.bbtweaks.bottlexp;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;

import java.util.Set;

public record OverviewSuggestion(
  int percentage,
  int experience,
  int bottleCount,
  int levelAfter
) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "percentage" -> percentage;
      case "experience" -> experience;
      case "level_after" -> levelAfter;
      case "bottle_count" -> bottleCount;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("percentage", "experience", "level_after", "bottle_count");
  }
}

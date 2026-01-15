package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record OverviewItem(String key, int amount) implements DirectFieldAccess {

  @Override
  public @Nullable Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "key" -> key;
      case "amount" -> amount;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("key", "amount");
  }
}

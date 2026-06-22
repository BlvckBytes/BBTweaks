package me.blvckbytes.bbtweaks.itemdata.display;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;

import java.util.Set;

public record PdcEntry(String key, String serializedValue) implements DirectFieldAccess {

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "key" -> key;
      case "value" -> serializedValue;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("key", "value");
  }
}

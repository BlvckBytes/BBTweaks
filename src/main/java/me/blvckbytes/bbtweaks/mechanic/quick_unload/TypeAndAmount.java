package me.blvckbytes.bbtweaks.mechanic.quick_unload;

import at.blvckbytes.component_markup.markup.interpreter.DirectFieldAccess;
import me.blvckbytes.bbtweaks.util.MutableInt;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record TypeAndAmount(Material material, int amount) implements DirectFieldAccess {

  public static List<TypeAndAmount> mapToList(Map<Material, MutableInt> map) {
    return map.entrySet().stream()
      .map(it -> new TypeAndAmount(it.getKey(), it.getValue().value))
      .toList();
  }

  @Override
  public Object accessField(String rawIdentifier) {
    return switch (rawIdentifier) {
      case "key" -> material.translationKey();
      case "amount" -> amount;
      default -> DirectFieldAccess.UNKNOWN_FIELD_SENTINEL;
    };
  }

  @Override
  public Set<String> getAvailableFields() {
    return Set.of("key", "amount");
  }
}

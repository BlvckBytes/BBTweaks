package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public record TypeExclusionRule(
  boolean onUncraftedItem,
  boolean onUncraftResult,
  Set<Material> materials,
  Set<Tag<Material>> tags,
  String reason
) implements TypeRule {
  public static TypeExclusionRule fromConfig(ConfigurationSection section) {
    var inclusionRule = TypeInclusionRule.fromConfig(section);
    var reason = section.getString("reason");

    if (reason == null || (reason = reason.trim()).isEmpty())
      throw new IllegalStateException("Missing a non-blank reason!");

    return new TypeExclusionRule(
      inclusionRule.onUncraftedItem(),
      inclusionRule.onUncraftResult(),
      inclusionRule.materials(),
      inclusionRule.tags(),
      reason
    );
  }
}

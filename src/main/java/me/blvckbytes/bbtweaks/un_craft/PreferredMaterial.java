package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;

public record PreferredMaterial(
  Set<Material> materials,
  Set<Tag<Material>> tags,
  Material preferredMaterial
) {
  public boolean matches(List<Material> choices) {
    for (var choice : choices) {
      var didMatchChoice = false;

      for (var tag : tags) {
        if (tag.isTagged(choice)) {
          didMatchChoice = true;
          break;
        }
      }

      if (!didMatchChoice && materials.contains(choice))
        didMatchChoice = true;

      // All choices need to find a match for this preferred material to take effect
      if (!didMatchChoice)
        return false;
    }

    return true;
  }

  public static PreferredMaterial fromConfig(ConfigurationSection section) {
    Material preferredMaterial;

    var preferredMaterialName = section.getString("preferredMaterial");

    if (preferredMaterialName == null)
      throw new IllegalStateException("Missing \"preferredMaterial\"-key");

    try {
      preferredMaterial = Material.valueOf(preferredMaterialName.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Unknown preferred material: " + preferredMaterialName);
    }

    var inclusionRule = TypeInclusionRule.fromConfig(section);

    return new PreferredMaterial(inclusionRule.materials(), inclusionRule.tags(), preferredMaterial);
  }
}

package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class PreferredMaterial extends TypeRule {

  public final Material preferredMaterial;

  public PreferredMaterial(ConfigurationSection section) {
    super(section);

    Material preferredMaterial;

    var preferredMaterialName = section.getString("preferredMaterial");

    if (preferredMaterialName == null)
      throw new IllegalStateException("Missing \"preferredMaterial\"-key");

    try {
      preferredMaterial = Material.valueOf(preferredMaterialName.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Unknown preferred material: " + preferredMaterialName);
    }

    this.preferredMaterial = preferredMaterial;
  }

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
}

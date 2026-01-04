package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Set;

public interface TypeRule {

  boolean onUncraftedItem();
  boolean onUncraftResult();
  Set<Material> materials();
  Set<Tag<Material>> tags();

  default boolean matches(Material material, MaterialType materialType) {
    if (materialType == MaterialType.UNCRAFTED_ITEM && !onUncraftedItem())
      return false;

    if (materialType == MaterialType.UNCRAFT_RESULT && !onUncraftResult())
      return false;

    if (materials().contains(material))
      return true;

    for (var tag : tags()) {
      if (tag.isTagged(material))
        return true;
    }

    return false;
  }
}

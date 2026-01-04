package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class IOTypeRule extends TypeRule {

  public final boolean onUnCraftedItem;
  public final boolean onUnCraftResult;

  public IOTypeRule(ConfigurationSection section) {
    super(section);

    this.onUnCraftedItem = section.getBoolean("onUnCraftedItem");
    this.onUnCraftResult = section.getBoolean("onUnCraftResult");
  }

  public boolean matches(Material material, MaterialType materialType) {
    if (materialType == MaterialType.UNCRAFTED_ITEM && !onUnCraftedItem)
      return false;

    if (materialType == MaterialType.UNCRAFT_RESULT && !onUnCraftResult)
      return false;

    if (materials.contains(material))
      return true;

    for (var tag : tags) {
      if (tag.isTagged(material))
        return true;
    }

    return false;
  }
}

package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ItemMaterialTagRegistry {

  private static final Map<String, Tag<Material>> itemMaterialTagByNameLower;

  static {
    itemMaterialTagByNameLower = new HashMap<>();

    for (var tagField : Tag.class.getFields()) {
      var modifiers = tagField.getModifiers();

      if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers))
        continue;

      if (tagField.getType() != Tag.class)
        continue;

      try {
        var tag = (Tag<?>) tagField.get(null);
        var values = tag.getValues();

        if (values.isEmpty())
          continue;

        var isItemMaterialTag = values.stream().allMatch(tagEntry -> {
          if (!(tagEntry instanceof Material material))
            return false;

          return material.isItem();
        });

        if (!isItemMaterialTag)
          continue;

        //noinspection unchecked
        itemMaterialTagByNameLower.put(tagField.getName().toLowerCase(), (Tag<Material>) tag);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Could not access tag-field", e);
      }
    }
  }

  public static @Nullable Tag<Material> getByName(String name) {
    return itemMaterialTagByNameLower.get(name.toLowerCase());
  }
}

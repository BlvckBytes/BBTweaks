package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class MaterialTagRegistry {

  private static final Map<String, Tag<Material>> itemMaterialTagByNameLower;
  private static final Map<String, Tag<Material>> blockMaterialTagByNameLower;

  static {
    itemMaterialTagByNameLower = new HashMap<>();
    blockMaterialTagByNameLower = new HashMap<>();

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

        var tagName = tagField.getName().toLowerCase();

        if (testAllMaterialsOf(tag, Material::isItem)) {
          //noinspection unchecked
          itemMaterialTagByNameLower.put(tagName, (Tag<Material>) tag);
        }

        if (testAllMaterialsOf(tag, Material::isBlock)) {
          //noinspection unchecked
          blockMaterialTagByNameLower.put(tagName, (Tag<Material>) tag);
        }
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Could not access tag-field", e);
      }
    }
  }

  private static boolean testAllMaterialsOf(Tag<?> tag, Predicate<Material> predicate) {
    for (var value : tag.getValues()) {
      if (!(value instanceof Material material))
        return false;

      if (!predicate.test(material))
        return false;
    }

    return true;
  }

  public static @Nullable Tag<Material> getItemTagByName(String name) {
    return itemMaterialTagByNameLower.get(name.toLowerCase());
  }

  public static @Nullable Tag<Material> getBlockTagByName(String name) {
    return blockMaterialTagByNameLower.get(name.toLowerCase());
  }
}

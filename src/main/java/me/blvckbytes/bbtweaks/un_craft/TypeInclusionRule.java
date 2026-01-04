package me.blvckbytes.bbtweaks.un_craft;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record TypeInclusionRule(
  boolean onUncraftedItem,
  boolean onUncraftResult,
  Set<Material> materials,
  Set<Tag<Material>> tags
) implements TypeRule {

  public static TypeInclusionRule fromConfig(ConfigurationSection section) {
    var onItem = section.getBoolean("onUncraftedItem");
    var onResult = section.getBoolean("onUncraftResult");

    var materials = new HashSet<Material>();

    for (var materialName : section.getStringList("materials")) {
      try {
        if (!materials.add(Material.valueOf(materialName.toUpperCase().trim())))
          throw new IllegalStateException("Duplicate material: " + materialName);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException("Unknown material: " + materialName);
      }
    }

    var materialPatterns = new ArrayList<Pattern>();

    for (var materialPattern : section.getStringList("materialPatterns")) {
      try {
        materialPatterns.add(Pattern.compile(materialPattern));
      } catch (PatternSyntaxException e) {
        throw new IllegalStateException("Malformed pattern \"" + materialPattern + "\"", e);
      }
    }

    var materialIsFlags = new HashSet<Method>();

    for (var isFlagName : section.getStringList("materialIsFlags")) {
      Method targetMethod = null;

      try {
        targetMethod = Material.class.getDeclaredMethod(isFlagName);
      } catch (NoSuchMethodException ignored) {}

      if (targetMethod == null)
        throw new IllegalStateException("Unknown is-flag: " + isFlagName);

      var modifiers = targetMethod.getModifiers();

      if (
        !Modifier.isPublic(modifiers)
          || Modifier.isStatic(modifiers)
          || targetMethod.getParameterCount() != 0
          || targetMethod.getReturnType() != boolean.class
      )
        throw new IllegalStateException("Unknown is-flag: " + isFlagName);

      if (!materialIsFlags.add(targetMethod))
        throw new IllegalStateException("Duplicate is-flag: " + isFlagName);
    }

    if (!materialPatterns.isEmpty() || !materialIsFlags.isEmpty()) {
      for (var material : Material.values()) {
        var materialName = material.name();

        var matches = false;

        for (var pattern : materialPatterns) {
          matches = pattern.matcher(materialName).matches();

          if (matches)
            break;
        }

        if (!matches) {
          for (var isFlag : materialIsFlags) {
            try {
              matches = (boolean) isFlag.invoke(material);
            } catch (Throwable e) {
              throw new IllegalStateException("Could not call isFlag-Method " + isFlag, e);
            }

            if (matches)
              break;
          }
        }

        if (!matches)
          continue;

        // Let's not be pedantic about set-semantics here, seeing how patterns/flags are
        // supposed to stay generic and may have some overlap with either each-other
        // or with previously defined materials; that's totally fine.
        materials.add(material);
      }
    }

    var tags = new HashSet<Tag<Material>>();

    for (var tagName : section.getStringList("tags")) {
      var tag = ItemMaterialTagRegistry.getByName(tagName.toLowerCase().trim());

      if (tag == null)
        throw new IllegalStateException("Unknown tag: " + tagName);

      if (!tags.add(tag))
        throw new IllegalStateException("Duplicate tag: " + tagName);
    }

    return new TypeInclusionRule(onItem, onResult,  materials, tags);
  }
}

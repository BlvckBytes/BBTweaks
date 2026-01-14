package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.un_craft.ItemMaterialTagRegistry;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TypeRule extends ConfigSection {

  public Set<Material> materials = new HashSet<>();

  public List<String> materialPatterns = new ArrayList<>();

  public List<String> materialIsFlags = new ArrayList<>();

  public Set<String> tags = new HashSet<>();

  @CSIgnore
  public Set<Tag<Material>> _tags = new HashSet<>();

  public TypeRule(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var tagName : tags) {
      var tag = ItemMaterialTagRegistry.getByName(tagName.toLowerCase().trim());

      if (tag == null)
        throw new IllegalStateException("Unknown tag: " + tagName);

      _tags.add(tag);
    }

    var patterns = mapMaterialPatterns();
    var methods = mapIsFlagMethods();

    if (!patterns.isEmpty() || !methods.isEmpty()) {
      for (var material : Material.values()) {
        var materialName = material.name();

        var matches = false;

        for (var pattern : patterns) {
          matches = pattern.matcher(materialName).matches();

          if (matches)
            break;
        }

        if (!matches) {
          for (var isFlag : methods) {
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
  }

  private List<Pattern> mapMaterialPatterns() {
    var patterns = new ArrayList<Pattern>();

    for (var materialPattern : materialPatterns) {
      try {
        patterns.add(Pattern.compile(materialPattern));
      } catch (PatternSyntaxException e) {
        throw new IllegalStateException("Malformed pattern \"" + materialPattern + "\"", e);
      }
    }

    return patterns;
  }

  private @NotNull Set<Method> mapIsFlagMethods() {
    var methods = new HashSet<Method>();

    for (var isFlagName : materialIsFlags) {
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

      if (!methods.add(targetMethod))
        throw new IllegalStateException("Duplicate is-flag: " + isFlagName);
    }
    return methods;
  }

  public boolean matches(Material material) {
    if (materials.contains(material))
      return true;

    for (var tag : _tags) {
      if (tag.isTagged(material))
        return true;
    }

    return false;
  }
}

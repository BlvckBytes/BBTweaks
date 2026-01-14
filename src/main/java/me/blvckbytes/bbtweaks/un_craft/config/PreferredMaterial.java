package me.blvckbytes.bbtweaks.un_craft.config;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;

import java.util.List;

public class PreferredMaterial extends TypeRule {

  public Material preferredMaterial;

  public PreferredMaterial(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public boolean matches(List<Material> choices) {
    for (var choice : choices) {
      var didMatchChoice = false;

      for (var tag : _tags) {
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

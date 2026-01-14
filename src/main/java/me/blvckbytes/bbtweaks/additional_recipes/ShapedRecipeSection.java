package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class ShapedRecipeSection extends ConfigSection {

  public Material result;
  public int amount;

  public List<ComponentMarkup> shape;
  public Map<String, IngredientSection> ingredients;

  public ShapedRecipeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (result == null)
      throw new MappingError("Absent \"result\"-property");
  }
}

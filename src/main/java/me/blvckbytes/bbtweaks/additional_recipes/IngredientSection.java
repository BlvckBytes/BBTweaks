package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;

public class IngredientSection extends ConfigSection {

  public Material type;

  public IngredientSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (type == null)
      throw new MappingError("Absent \"type\"-property");
  }
}

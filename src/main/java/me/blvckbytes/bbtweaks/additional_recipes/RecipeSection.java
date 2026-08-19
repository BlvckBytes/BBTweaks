package me.blvckbytes.bbtweaks.additional_recipes;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;

public abstract class RecipeSection extends ConfigSection {

  public Material result;
  public int amount;

  public RecipeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (result == null)
      throw new MappingError("Absent \"result\"-property");

    if (amount <= 0 || amount > result.getMaxStackSize())
      throw new MappingError("The \"amount\" cannot be less than or equal to zero or greater than the max-stack-size");
  }
}

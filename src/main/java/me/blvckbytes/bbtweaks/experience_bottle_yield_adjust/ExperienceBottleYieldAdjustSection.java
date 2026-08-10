package me.blvckbytes.bbtweaks.experience_bottle_yield_adjust;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class ExperienceBottleYieldAdjustSection extends ConfigSection {

  public boolean enabled;
  public int experience;

  public ExperienceBottleYieldAdjustSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (experience <= 0)
      throw new MappingError("The property \"experience\" cannot be less than or equal to zero");
  }
}

package me.blvckbytes.bbtweaks.multi_break.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class MultiBreakLimitsSection extends ConfigSection {

  public int maxVolume;
  public int maxExtent;

  public MultiBreakLimitsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (maxVolume <= 0)
      throw new MappingError("Property \"maxVolume\" cannot be less than or equal to zero");

    if (maxExtent <= 0)
      throw new MappingError("Property \"maxExtent\" cannot be less than or equal to zero");
  }
}

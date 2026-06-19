package me.blvckbytes.bbtweaks.lava_sponge;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class LimitsSection extends ConfigSection {

  public @CSIgnore String tierName;

  public int maxDistance;
  public int maxBlocks;

  public LimitsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    if (maxDistance <= 0)
      throw new MappingError("Property \"maxDistance\" cannot be less than or equal to zero");

    if (maxBlocks <= 0)
      throw new MappingError("Property \"maxBlocks\" cannot be less than or equal to zero");
  }
}

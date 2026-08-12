package me.blvckbytes.bbtweaks.worldguard_flags;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class WorldGuardFlagsSection extends ConfigSection {

  public int unusedVehicleDurationSeconds;

  public WorldGuardFlagsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (unusedVehicleDurationSeconds <= 0)
      throw new MappingError("Property \"unusedVehicleDurationSeconds\" cannot be less than or equal to zero");
  }
}

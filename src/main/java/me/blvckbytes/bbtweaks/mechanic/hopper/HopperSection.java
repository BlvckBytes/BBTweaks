package me.blvckbytes.bbtweaks.mechanic.hopper;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class HopperSection extends ConfigSection {

  public int transportCycleTicks;
  public ComponentMarkup noPermission;
  public ComponentMarkup notOnAHopper;
  public ComponentMarkup multipleHoppers;
  public ComponentMarkup creationSuccess;

  public HopperSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (transportCycleTicks <= 0)
      throw new MappingError("\"transportCycleTicks\" cannot be less than or equal to zero");
  }
}

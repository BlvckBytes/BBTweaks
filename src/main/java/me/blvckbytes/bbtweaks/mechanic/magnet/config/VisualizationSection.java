package me.blvckbytes.bbtweaks.mechanic.magnet.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import me.blvckbytes.bbtweaks.mechanic.magnet.ColorSection;

import java.lang.reflect.Field;
import java.util.List;

public class VisualizationSection extends ConfigSection {

  public int durationMs;
  public int periodTicks;
  public double stepSize;
  public double dustSize;
  public int editModeMaxXZDistance;

  public @CSAlways ColorSection visualizeColor;
  public @CSAlways ColorSection editColor;
  public @CSAlways ColorSection editHighlightColor;

  public VisualizationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (editModeMaxXZDistance <= 0)
      throw new MappingError("\"editModeMaxXZDistance\" cannot be less than or equal to zero");

    if (periodTicks <= 0)
      throw new MappingError("\"periodTicks\" cannot be less than or equal to zero");

    if (durationMs <= 0)
      throw new MappingError("\"durationMs\" cannot be less than or equal to zero");

    if (stepSize <= 0 || stepSize > 1)
      throw new MappingError("\"stepSize\" cannot be less than or equal to zero or greater than one");

    if (dustSize <= 0)
      throw new MappingError("\"dustSize\" cannot be less than or equal to zero");
  }
}

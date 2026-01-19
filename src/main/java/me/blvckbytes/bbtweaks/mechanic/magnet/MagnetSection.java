package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class MagnetSection extends ConfigSection {

  public int maxWidthOrDepth;
  public int maxHeight;
  public int collectionPeriodTicks;
  public int defaultWidthAndDepth;
  public int defaultHeight;
  public int defaultOffsetX;
  public int defaultOffsetY;
  public int defaultOffsetZ;

  public @CSAlways VisualizationSection visualization;

  public MagnetSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (maxWidthOrDepth <= 0)
      throw new MappingError("\"maxWidthOrDepth\" cannot be less than or equal to zero");

    if (maxHeight <= 0)
      throw new MappingError("\"maxHeight\" cannot be less than or equal to zero");

    if (defaultWidthAndDepth <= 0)
      throw new MappingError("\"defaultWidthAndDepth\" cannot be less than or equal to zero");

    if (defaultHeight <= 0)
      throw new MappingError("\"defaultHeight\" cannot be less than or equal to zero");

    if (collectionPeriodTicks <= 0)
      throw new MappingError("\"collectionPeriodTicks\" cannot be less than or equal to zero");
  }
}

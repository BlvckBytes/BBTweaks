package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Color;

import java.lang.reflect.Field;
import java.util.List;

public class VisualizationSection extends ConfigSection {

  public int durationMs;
  public int periodTicks;
  public double stepSize;
  public int colorRed;
  public int colorGreen;
  public int colorBlue;
  public double dustSize;

  public Color _color;

  public VisualizationSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (periodTicks <= 0)
      throw new MappingError("\"periodTicks\" cannot be less than or equal to zero");

    if (durationMs <= 0)
      throw new MappingError("\"durationMs\" cannot be less than or equal to zero");

    if (stepSize <= 0 || stepSize > 1)
      throw new MappingError("\"stepSize\" cannot be less than or equal to zero or greater than one");

    if (1.0 % stepSize != 0)
      throw new MappingError("\"stepSize\" must measure 1 without remainder");

    if (colorRed < 0 || colorRed > 255)
      throw new MappingError("\"colorRed\" must be between 0 and 255");

    if (colorGreen < 0 || colorGreen > 255)
      throw new MappingError("\"colorGreen\" must be between 0 and 255");

    if (colorBlue < 0 || colorBlue > 255)
      throw new MappingError("\"colorBlue\" must be between 0 and 255");

    _color = Color.fromRGB(colorRed, colorGreen, colorBlue);

    if (dustSize <= 0)
      throw new MappingError("\"dustSize\" cannot be less than or equal to zero");
  }
}

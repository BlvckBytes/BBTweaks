package me.blvckbytes.bbtweaks.mechanic.magnet;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Color;

import java.lang.reflect.Field;
import java.util.List;

public class ColorSection extends ConfigSection {

  public int red;
  public int green;
  public int blue;

  public Color _color;

  public ColorSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (red < 0 || red > 255)
      throw new MappingError("\"red\" must be between 0 and 255");

    if (green < 0 || green > 255)
      throw new MappingError("\"green\" must be between 0 and 255");

    if (blue < 0 || blue > 255)
      throw new MappingError("\"blue\" must be between 0 and 255");

    _color = Color.fromRGB(red, green, blue);
  }
}

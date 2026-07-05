package me.blvckbytes.bbtweaks.sidebar.config;

import at.blvckbytes.cm_mapper.MaterialMatcher;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import org.bukkit.Material;

import java.lang.reflect.Field;
import java.util.List;

public class ColorSection extends ConfigSection {

  public ComponentMarkup color;
  public ComponentMarkup displayName;
  public ComponentMarkup iconType;

  public @CSIgnore Material _iconType;

  public ColorSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    var materialString = iconType.asPlainString(null);
    var material = MaterialMatcher.tryMatch(materialString);

    if (material == null)
      throw new MappingError("Unknown Material \"" + materialString + "\" on property \"iconType\"");

    _iconType = material;
  }
}

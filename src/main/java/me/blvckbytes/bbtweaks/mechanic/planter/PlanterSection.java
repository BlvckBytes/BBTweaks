package me.blvckbytes.bbtweaks.mechanic.planter;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class PlanterSection extends ConfigSection {

  public int maximumRadius;

  public ComponentMarkup noPermission;
  public ComponentMarkup missingRadius;
  public ComponentMarkup nonPositiveRadius;
  public ComponentMarkup exceededMaximumRadius;
  public ComponentMarkup creationSuccess;

  public PlanterSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (maximumRadius <= 0)
      throw new MappingError("Property \"maximumRadius\" cannot be less than or equal to zero");
  }
}

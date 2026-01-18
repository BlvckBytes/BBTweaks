package me.blvckbytes.bbtweaks.mechanic.clock;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class ClockSection extends ConfigSection {

  public ComponentExpression minTickPeriod;
  public int _minTickPeriod;

  public ComponentMarkup noPermission;
  public ComponentMarkup periodDurationAbsent;
  public ComponentMarkup periodDurationNoPositiveInt;
  public ComponentMarkup periodDurationTooLow;
  public ComponentMarkup periodDurationUneven;
  public ComponentMarkup creationSuccess;

  public ClockSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    _minTickPeriod = ComponentExpression.asInt(minTickPeriod, null);
  }
}

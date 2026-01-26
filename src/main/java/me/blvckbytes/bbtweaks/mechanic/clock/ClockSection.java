package me.blvckbytes.bbtweaks.mechanic.clock;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class ClockSection extends ConfigSection {

  public int minTickPeriod;

  public ComponentMarkup noPermission;
  public ComponentMarkup periodDurationAbsent;
  public ComponentMarkup periodDurationNoPositiveInt;
  public ComponentMarkup periodDurationTooLow;
  public ComponentMarkup periodDurationUneven;
  public ComponentMarkup creationSuccess;
  public ComponentMarkup unknownRemainingTime;
  public ComponentMarkup remainingTimeActionBar;

  public ClockSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (minTickPeriod <= 0 || minTickPeriod % 2 != 0)
      throw new MappingError("\"minTickPeriod\" must be greater than zero and has to be divisible by two");
  }
}

package me.blvckbytes.bbtweaks.mechanic.pulse_extender;

import at.blvckbytes.cm_mapper.cm.ComponentExpression;
import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class PulseExtenderSection extends ConfigSection {

  public int minSignalLength;

  public ComponentMarkup noPermission;
  public ComponentMarkup signalLengthAbsent;
  public ComponentMarkup signalLengthNoPositiveInt;
  public ComponentMarkup signalLengthTooLow;
  public ComponentMarkup creationSuccess;

  public PulseExtenderSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (minSignalLength <= 0)
      throw new MappingError("\"minSignalLength\" must be greater than zero");
  }
}

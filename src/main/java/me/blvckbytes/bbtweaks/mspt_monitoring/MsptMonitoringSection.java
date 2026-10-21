package me.blvckbytes.bbtweaks.mspt_monitoring;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class MsptMonitoringSection extends ConfigSection {

  public int apiServerPort;
  public int retentionDurationSeconds;

  public MsptMonitoringSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (retentionDurationSeconds < 60 * 60)
      throw new MappingError("Property \"retentionDurationSeconds\" must at least be an hour");
  }
}

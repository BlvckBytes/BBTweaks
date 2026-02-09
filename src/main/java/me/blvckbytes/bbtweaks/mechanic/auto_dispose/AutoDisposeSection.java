package me.blvckbytes.bbtweaks.mechanic.auto_dispose;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class AutoDisposeSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup noContainer;
  public ComponentMarkup existingSign;
  public ComponentMarkup creationSuccess;
  public int clearIntervalTicks;

  public AutoDisposeSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (clearIntervalTicks <= 0)
      throw new MappingError("\"clearIntervalTicks\" cannot be <= 0");
  }
}

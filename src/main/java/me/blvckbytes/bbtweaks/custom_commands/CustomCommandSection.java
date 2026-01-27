package me.blvckbytes.bbtweaks.custom_commands;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

import java.lang.reflect.Field;
import java.util.List;

public class CustomCommandSection extends CommandSection {

  public ComponentMarkup message;

  public CustomCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(null, baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    if (evaluatedName == null || evaluatedName.isBlank())
      throw new MappingError("\"name\" must not be absent or blank");

    if (message == null)
      throw new MappingError("\"message\" is mandatory, but missing");
  }
}

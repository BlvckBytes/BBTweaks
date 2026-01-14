package me.blvckbytes.bbtweaks.get_uuid;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class GetUuidSection extends ConfigSection {

  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup unknownName;
  public ComponentMarkup resultMessage;

  public GetUuidSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
